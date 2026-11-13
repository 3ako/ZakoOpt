package zako.opt.text;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.experimental.UtilityClass;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import zako.opt.MixinPlugin;
import net.minecraft.world.level.block.entity.BlockEntity;
import zako.opt.feature.Features;
import zako.opt.particle.CameraPosition;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

// sign text baked per chunk section into GPU buffers on the render thread. A sign's lines are captured once in section
// space, either from its own submit (signs still drawn as block entities) or, for signs whose board went into Sodium's
// mesh, by running the renderer once on its own; from then on a section is one draw per glyph page
@UtilityClass
public class SignTextMesh {
	private final int REBUILD_BUDGET = 8192;
	private final int OUTLINE_DISTANCE_SQ = 16 * 16;
	private final int BLACK = DyeColor.BLACK.getTextColor();
	private final Long2ObjectOpenHashMap<Section> SECTIONS = new Long2ObjectOpenHashMap<>();
	private final Queue<Object[]> QUEUE = new ConcurrentLinkedQueue<>();
	private final SubmitNodeStorage SINK = new SubmitNodeStorage();
	private final CameraRenderState NO_CAMERA = new CameraRenderState();
	private ClientLevel level;
	private int generation;
	private long frame;
	private boolean armed, skip, capturing;
	private Entry capture;
	private final Vector3f shift = new Vector3f();

	private record Line(Matrix4f pose, float x, float y, FormattedCharSequence text, int light, int color, int outline) {
		// black glowing text is always outlined, other glowing text only up close: that one gets its own range, drawn by distance
		boolean bakedOutline() {
			return outline != 0 && color == BLACK;
		}
	}

	private record Mesh(RenderType type, GpuBuffer vertices, int indexCount) {
	}

	private record Draw(Mesh mesh, GpuBufferSlice transform, int baseVertex, int indexCount) {
	}

	private static final class Entry {
		final BlockPos pos;
		Object state;
		SignText front, back;
		int light;
		boolean filtering, owned;
		final List<Line> lines = new ArrayList<>(8);
		// this sign's up-close outline in the section's outline meshes: {mesh index, first vertex, vertex count}
		final List<int[]> outline = new ArrayList<>(1);

		Entry(BlockPos pos) {
			this.pos = pos;
		}

		boolean matches(SignRenderState s) {
			return state == s.transformations && front == s.frontText && back == s.backText && light == s.lightCoords && filtering == s.isTextFilteringEnabled;
		}

		void set(SignRenderState s) {
			state = s.transformations;
			front = s.frontText;
			back = s.backText;
			light = s.lightCoords;
			filtering = s.isTextFilteringEnabled;
			lines.clear();
		}
	}

	private static final class Section {
		final long key;
		final int x, y, z;
		final Long2ObjectOpenHashMap<Entry> entries = new Long2ObjectOpenHashMap<>();
		List<Mesh> meshes = List.of(), outlineMeshes = List.of();
		List<Entry> outlined = List.of();
		boolean dirty = true, owned;
		long seenFrame = -1, drawFrame = -1;

		Section(long key) {
			this.key = key;
			x = SectionPos.sectionToBlockCoord(SectionPos.x(key));
			y = SectionPos.sectionToBlockCoord(SectionPos.y(key));
			z = SectionPos.sectionToBlockCoord(SectionPos.z(key));
		}

		void free() {
			for (Mesh mesh : meshes) {
				mesh.vertices.close();
			}
			for (Mesh mesh : outlineMeshes) {
				mesh.vertices.close();
			}
			meshes = outlineMeshes = List.of();
		}
	}

	private boolean on() {
		return Features.SIGN_CACHE.on && !Features.shaders() && !Minecraft.getInstance().gameRenderer.useImprovedTransparency();
	}

	// builder threads: the board is a block model already, so the sign leaves the block entity list and its text is captured on the render thread
	public boolean bake(BlockEntity blockEntity, Object renderer) {
		if (!(blockEntity instanceof SignBlockEntity sign) || !(renderer instanceof AbstractSignRenderer<?> signRenderer) || !on()) {
			return false;
		}
		queue(sign, signRenderer);
		return true;
	}

	private void queue(SignBlockEntity sign, AbstractSignRenderer<?> renderer) {
		QUEUE.add(new Object[]{sign, renderer});
	}

	// a sign whose board is baked gets no submits, so a text change is caught here
	public void changed(SignBlockEntity sign) {
		if (sign.getLevel() == null || !sign.getLevel().isClientSide() || !RenderSystem.isOnRenderThread()) {
			return;
		}
		Section section = SECTIONS.get(SectionPos.asLong(sign.getBlockPos()));
		Entry entry = section == null ? null : section.entries.get(sign.getBlockPos().asLong());
		if (entry != null && entry.owned && Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(sign) instanceof Object o && o instanceof AbstractSignRenderer renderer) {
			queue(sign, renderer);
		}
	}

	public void arm() {
		armed = true;
		if (!on()) {
			QUEUE.clear();
			return;
		}
		validate();
		for (Object[] item; (item = QUEUE.poll()) != null; ) {
			own((SignBlockEntity) item[0], (AbstractSignRenderer) item[1]);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void own(SignBlockEntity sign, AbstractSignRenderer renderer) {
		if (sign.isRemoved() || sign.getLevel() != level) {
			return;
		}
		BlockEntityRenderer raw = renderer;
		SignRenderState state = (SignRenderState) raw.createRenderState();
		raw.extractRenderState(sign, state, 0.0F, Vec3.ZERO, null);
		state.drawOutline = true;
		BlockPos pos = sign.getBlockPos();
		Section section = section(SectionPos.asLong(pos));
		Entry entry = section.entries.computeIfAbsent(pos.asLong(), k -> new Entry(pos.immutable()));
		entry.set(state);
		entry.owned = true;
		section.dirty = true;
		PoseStack poseStack = new PoseStack();
		poseStack.translate(pos.getX() - section.x, pos.getY() - section.y, pos.getZ() - section.z);
		capturing = true;
		skip = false;
		capture = entry;
		shift.zero();
		try {
			raw.submit(state, poseStack, SINK, NO_CAMERA);
		} finally {
			capturing = false;
			capture = null;
			SINK.getSubmitsPerOrder().clear();
		}
	}

	private Section section(long key) {
		Section section = SECTIONS.get(key);
		if (section == null) {
			SECTIONS.put(key, section = new Section(key));
		}
		return section;
	}

	// before the renderer submits a sign: its text is either left to the section mesh, or drawn vanilla and captured
	public void begin(SignRenderState state, Vec3 camera) {
		if (capturing) {
			return;
		}
		skip = false;
		capture = null;
		if (!on()) {
			return;
		}
		validate();
		BlockPos pos = state.blockPos;
		Section section = section(SectionPos.asLong(pos));
		section.seenFrame = frame;
		Entry entry = section.entries.get(pos.asLong());
		if (entry != null && entry.matches(state)) {
			entry.owned = false;
			if (!section.dirty) {
				section.drawFrame = frame;
				// the up-close outline of coloured glowing text is drawn by vanilla, over the baked text
				skip = !(state.drawOutline && (outlinedUpClose(state.frontText) || outlinedUpClose(state.backText)));
			}
			return;
		}
		if (entry == null) {
			section.entries.put(pos.asLong(), entry = new Entry(pos.immutable()));
		}
		entry.set(state);
		entry.owned = false;
		section.dirty = true;
		capture = entry;
		shift.set((float) (camera.x - section.x), (float) (camera.y - section.y), (float) (camera.z - section.z));
	}

	private boolean outlinedUpClose(SignText text) {
		return text != null && text.hasGlowingText() && text.getColor().getTextColor() != BLACK;
	}

	public boolean skip() {
		return skip;
	}

	public void line(Matrix4f pose, float x, float y, FormattedCharSequence text, int light, int color, int outline) {
		if (capture != null) {
			capture.lines.add(new Line(new Matrix4f().translation(shift).mul(pose), x, y, text, light, color, outline));
		}
	}

	public void end() {
		if (!capturing) {
			skip = false;
			capture = null;
		}
	}

	public void remove(BlockPos pos) {
		long key = SectionPos.asLong(pos);
		Section section = SECTIONS.get(key);
		if (section != null && section.entries.remove(pos.asLong()) != null) {
			section.dirty = true;
			if (section.entries.isEmpty()) {
				section.free();
				SECTIONS.remove(key);
			}
		}
	}

	// after the main pass's features: rebuild what changed, then draw every section left to its mesh
	public void draw(RenderPass pass) {
		if (!armed) {
			return;
		}
		armed = false;
		long now = frame++;
		if (SECTIONS.isEmpty() || !on()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Vec3 camera = CameraPosition.get();
		SodiumWorldRenderer sodium = MixinPlugin.SODIUM ? SodiumWorldRenderer.instanceNullable() : null;
		int budget = REBUILD_BUDGET;
		List<Section> draw = new ArrayList<>();
		for (Section section : SECTIONS.values()) {
			if (section.dirty) {
				if (budget <= 0) {
					continue;
				}
				budget -= section.entries.size();
				rebuild(section);
				// signs submitted this frame were drawn vanilla while the section was dirty
				if (section.seenFrame == now) {
					continue;
				}
			}
			if (section.drawFrame == now || section.owned && sodium != null && sodium.isBoxVisible(section.x, section.y, section.z, section.x + 16, section.y + 16, section.z + 16)) {
				draw.add(section);
			}
		}
		if (!draw.isEmpty()) {
			render(draw, camera, mc, pass);
		}
	}

	private void validate() {
		ClientLevel now = Minecraft.getInstance().level;
		if (now != level || generation != PreparedTextCache.generation()) {
			for (Section section : SECTIONS.values()) {
				section.free();
			}
			SECTIONS.clear();
			level = now;
			generation = PreparedTextCache.generation();
		}
	}

	private void rebuild(Section section) {
		section.free();
		Font font = Minecraft.getInstance().font;
		Map<RenderType, BufferBuilder> builders = new IdentityHashMap<>();
		List<ByteBufferBuilder> memory = new ArrayList<>();
		Recorder recorder = new Recorder(builders, memory, 1 << 16);
		boolean owned = false;
		// a sign gone from the level without reaching remove() (seen when a player breaks one) must not leave its text behind
		section.entries.values().removeIf(entry -> !(level.getBlockEntity(entry.pos) instanceof SignBlockEntity sign) || sign.isRemoved());
		try {
			for (Entry entry : section.entries.values()) {
				owned |= entry.owned;
				for (Line line : entry.lines) {
					recorder.draw(font, line, line.bakedOutline());
				}
			}
			List<Mesh> meshes = new ArrayList<>(builders.size());
			for (Map.Entry<RenderType, BufferBuilder> e : builders.entrySet()) {
				try (MeshData mesh = e.getValue().build()) {
					if (mesh != null) {
						GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "ZakoOpt sign text", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
						meshes.add(new Mesh(e.getKey(), buffer, mesh.drawState().indexCount()));
					}
				}
			}
			section.meshes = meshes;
			section.owned = owned;
			bakeOutlines(section, font);
			section.dirty = false;
		} finally {
			for (ByteBufferBuilder bytes : memory) {
				bytes.close();
			}
		}
	}

	// the outline coloured glowing text gets within 16 blocks: every such sign recorded once into its own vertex range
	private void bakeOutlines(Section section, Font font) {
		Map<RenderType, Integer> index = new IdentityHashMap<>();
		List<RenderType> types = new ArrayList<>();
		List<List<ByteBuffer>> parts = new ArrayList<>();
		List<int[]> vertices = new ArrayList<>();
		List<Entry> outlined = new ArrayList<>();
		for (Entry entry : section.entries.values()) {
			entry.outline.clear();
			if (!entry.owned) {
				continue;
			}
			Map<RenderType, BufferBuilder> builders = new IdentityHashMap<>();
			List<ByteBufferBuilder> memory = new ArrayList<>();
			Recorder recorder = new Recorder(builders, memory, 4096);
			try {
				for (Line line : entry.lines) {
					if (line.outline != 0 && !line.bakedOutline()) {
						recorder.draw(font, line, true);
					}
				}
				for (Map.Entry<RenderType, BufferBuilder> e : builders.entrySet()) {
					try (MeshData mesh = e.getValue().build()) {
						if (mesh == null) {
							continue;
						}
						int i = index.computeIfAbsent(e.getKey(), t -> {
							types.add(t);
							parts.add(new ArrayList<>());
							vertices.add(new int[1]);
							return types.size() - 1;
						});
						ByteBuffer src = mesh.vertexBuffer();
						ByteBuffer copy = ByteBuffer.allocateDirect(src.remaining());
						copy.put(src).flip();
						parts.get(i).add(copy);
						int count = mesh.drawState().vertexCount();
						entry.outline.add(new int[]{i, vertices.get(i)[0], count});
						vertices.get(i)[0] += count;
					}
				}
			} finally {
				for (ByteBufferBuilder bytes : memory) {
					bytes.close();
				}
			}
			if (!entry.outline.isEmpty()) {
				outlined.add(entry);
			}
		}
		section.outlined = outlined;
		if (outlined.isEmpty()) {
			return;
		}
		List<Mesh> meshes = new ArrayList<>(types.size());
		for (int i = 0; i < types.size(); i++) {
			int size = 0;
			for (ByteBuffer part : parts.get(i)) {
				size += part.remaining();
			}
			ByteBuffer all = MemoryUtil.memAlloc(size);
			try {
				for (ByteBuffer part : parts.get(i)) {
					all.put(part);
				}
				all.flip();
				GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "ZakoOpt sign outline", GpuBuffer.USAGE_VERTEX, all);
				meshes.add(new Mesh(types.get(i), buffer, vertices.get(i)[0] / 4 * 6));
			} finally {
				MemoryUtil.memFree(all);
			}
		}
		section.outlineMeshes = meshes;
	}

	private Matrix4f toCamera(Section section, Vec3 camera) {
		return new Matrix4f().translation((float) (section.x - camera.x), (float) (section.y - camera.y), (float) (section.z - camera.z));
	}

	private GpuBufferSlice transform(Section section, Vec3 camera) {
		return RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy().mul(toCamera(section, camera)));
	}

	private void render(List<Section> sections, Vec3 camera, Minecraft mc, RenderPass pass) {
		List<Draw> draws = new ArrayList<>();
		List<Draw> outlines = new ArrayList<>();
		boolean scoping = mc.player != null && mc.options.getCameraType().isFirstPerson() && mc.player.isScoping();
		for (Section section : sections) {
			if (section.meshes.isEmpty()) {
				continue;
			}
			GpuBufferSlice transform = transform(section, camera);
			for (Mesh mesh : section.meshes) {
				draws.add(new Draw(mesh, transform, 0, mesh.indexCount));
			}
			if (section.outlined.isEmpty() || !scoping && camera.distanceToSqr(section.x + 8, section.y + 8, section.z + 8) > 30 * 30) {
				continue;
			}
			for (Entry entry : section.outlined) {
				double dx = entry.pos.getX() + 0.5 - camera.x, dy = entry.pos.getY() + 0.5 - camera.y, dz = entry.pos.getZ() + 0.5 - camera.z;
				if (!scoping && dx * dx + dy * dy + dz * dz >= OUTLINE_DISTANCE_SQ) {
					continue;
				}
				for (int[] range : entry.outline) {
					outlines.add(new Draw(section.outlineMeshes.get(range[0]), transform, range[1], range[2] / 4 * 6));
				}
			}
		}
		submit(draws, pass);
		submit(outlines, pass);
	}

	private void submit(List<Draw> draws, RenderPass pass) {
		Map<RenderType, List<Draw>> byType = new IdentityHashMap<>();
		for (Draw draw : draws) {
			byType.computeIfAbsent(draw.mesh.type, t -> new ArrayList<>()).add(draw);
		}
		for (Map.Entry<RenderType, List<Draw>> e : byType.entrySet()) {
			RenderType type = e.getKey();
			int maxIndices = 0;
			for (Draw draw : e.getValue()) {
				maxIndices = Math.max(maxIndices, draw.indexCount);
			}
			RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(type.primitiveTopology());
			GpuBuffer indices = sequential.getBuffer(maxIndices);
			PreparedRenderType prepared = type.prepare();
			pass.setPipeline(RenderSystem.getCompiledPipeline(prepared.pipeline()));
			RenderSystem.bindDefaultUniforms(pass);
			for (PreparedRenderType.Texture texture : prepared.textures()) {
				pass.setUniform(texture.name(), texture.textureView(), texture.sampler());
			}
			pass.setIndexBuffer(indices, sequential.type());
			for (Draw draw : e.getValue()) {
				pass.setUniform("DynamicTransforms", draw.transform);
				pass.setVertexBuffer(0, draw.mesh.vertices.slice());
				pass.drawIndexed(draw.indexCount, 1, 0, draw.baseVertex, 0);
			}
		}
	}

	// what TextFeatureRenderer does with a text submit, recorded into per-type builders
	private static final class Recorder implements Font.GlyphVisitor {
		final Map<RenderType, BufferBuilder> builders;
		final List<ByteBufferBuilder> memory;
		final int size;
		Matrix4f pose;
		int light;
		Font.DisplayMode mode;

		Recorder(Map<RenderType, BufferBuilder> builders, List<ByteBufferBuilder> memory, int size) {
			this.builders = builders;
			this.memory = memory;
			this.size = size;
		}

		void draw(Font font, Line line, boolean outline) {
			pose = line.pose;
			light = line.light;
			if (outline) {
				mode = Font.DisplayMode.NORMAL;
				font.prepare8xTextOutline(line.text, line.x, line.y, line.outline).visit(this);
			}
			mode = Font.DisplayMode.POLYGON_OFFSET;
			font.prepareText(line.text, line.x, line.y, line.color, false, false, 0).visit(this);
		}

		@Override
		public void acceptRenderable(TextRenderable renderable) {
			RenderType type = renderable.renderType(mode);
			BufferBuilder builder = builders.computeIfAbsent(type, t -> {
				ByteBufferBuilder bytes = new ByteBufferBuilder(size);
				memory.add(bytes);
				return new BufferBuilder(bytes, t.primitiveTopology(), t.format());
			});
			renderable.render(pose, builder, light, false);
		}
	}
}
