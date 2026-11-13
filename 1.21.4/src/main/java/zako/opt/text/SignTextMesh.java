package zako.opt.text;

import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.experimental.UtilityClass;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import zako.opt.MixinPlugin;
import zako.opt.feature.Features;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

// sign text baked per chunk section into vertex buffers on the render thread. A sign's lines are captured once in section
// space, either from its own render (signs still drawn as block entities) or, for signs whose board went into Sodium's
// mesh, by running the renderer once on its own; from then on a section is one draw per glyph page
@UtilityClass
public class SignTextMesh {
	private final int REBUILD_BUDGET = 8192;
	private final int OUTLINE_DISTANCE_SQ = 16 * 16;
	private final int BLACK = DyeColor.BLACK.getTextColor();
	private final Long2ObjectOpenHashMap<Section> SECTIONS = new Long2ObjectOpenHashMap<>();
	private final Queue<Object[]> QUEUE = new ConcurrentLinkedQueue<>();
	private ClientLevel level;
	private int generation;
	private long frame;
	private boolean armed, skip, capturing;
	private Entry capture;
	private final Vector3f shift = new Vector3f();

	private record Line(Matrix4f pose, float x, float y, FormattedCharSequence text, int light, int color, int outline) {
		// black glowing text is always outlined, other glowing text only up close: that one gets its own buffers, drawn by distance
		boolean bakedOutline() {
			return outline != 0 && color == BLACK;
		}
	}

	private record Mesh(RenderType type, VertexBuffer buffer) {
	}

	private record Draw(Mesh mesh, Matrix4f modelView) {
	}

	private static final class Entry {
		final BlockPos pos;
		BlockState state;
		SignText front, back;
		int light;
		boolean filtering, owned;
		final List<Line> lines = new ArrayList<>(8);
		List<Mesh> outline = List.of();

		Entry(BlockPos pos) {
			this.pos = pos;
		}

		boolean matches(SignBlockEntity sign, int light, boolean filtering) {
			return state == sign.getBlockState() && front == sign.getFrontText() && back == sign.getBackText() && this.light == light && this.filtering == filtering;
		}

		void set(SignBlockEntity sign, int light, boolean filtering) {
			state = sign.getBlockState();
			front = sign.getFrontText();
			back = sign.getBackText();
			this.light = light;
			this.filtering = filtering;
			lines.clear();
		}

		void freeOutline() {
			for (Mesh mesh : outline) {
				mesh.buffer.close();
			}
			outline = List.of();
		}
	}

	private static final class Section {
		final long key;
		final int x, y, z;
		final Long2ObjectOpenHashMap<Entry> entries = new Long2ObjectOpenHashMap<>();
		List<Mesh> meshes = List.of();
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
				mesh.buffer.close();
			}
			meshes = List.of();
			for (Entry entry : entries.values()) {
				entry.freeOutline();
			}
			outlined = List.of();
		}
	}

	private boolean on() {
		return Features.SIGN_CACHE.on && !Features.shaders();
	}

	// builder threads: the board went into the chunk mesh, the text is captured on the render thread
	public void queue(SignBlockEntity sign, AbstractSignRenderer renderer) {
		QUEUE.add(new Object[]{sign, renderer});
	}

	// a sign whose board is baked is not rendered, so a text change is caught here
	public void changed(SignBlockEntity sign) {
		if (sign.getLevel() == null || !sign.getLevel().isClientSide() || !RenderSystem.isOnRenderThread()) {
			return;
		}
		Section section = SECTIONS.get(SectionPos.asLong(sign.getBlockPos()));
		Entry entry = section == null ? null : section.entries.get(sign.getBlockPos().asLong());
		if (entry != null && entry.owned && Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(sign) instanceof AbstractSignRenderer renderer) {
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

	private void own(SignBlockEntity sign, AbstractSignRenderer renderer) {
		if (sign.isRemoved() || sign.getLevel() != level) {
			return;
		}
		BlockPos pos = sign.getBlockPos();
		int light = LevelRenderer.getLightColor(level, pos);
		Section section = section(SectionPos.asLong(pos));
		Entry entry = section.entries.computeIfAbsent(pos.asLong(), k -> new Entry(pos.immutable()));
		entry.set(sign, light, Minecraft.getInstance().isTextFilteringEnabled());
		entry.owned = true;
		section.dirty = true;
		PoseStack poseStack = new PoseStack();
		poseStack.translate(pos.getX() - section.x, pos.getY() - section.y, pos.getZ() - section.z);
		capturing = true;
		skip = false;
		capture = entry;
		shift.zero();
		try {
			// nothing is drawn while capturing: the board and the text calls are dropped, so no buffers are needed
			renderer.render(sign, 0.0F, poseStack, null, light, OverlayTexture.NO_OVERLAY);
		} finally {
			capturing = false;
			capture = null;
		}
	}

	private Section section(long key) {
		Section section = SECTIONS.get(key);
		if (section == null) {
			SECTIONS.put(key, section = new Section(key));
		}
		return section;
	}

	// before the renderer draws a sign: its text is either left to the section mesh, or drawn vanilla and captured
	public void begin(SignBlockEntity sign, int light) {
		if (capturing) {
			return;
		}
		skip = false;
		capture = null;
		if (!on()) {
			return;
		}
		validate();
		Minecraft mc = Minecraft.getInstance();
		boolean filtering = mc.isTextFilteringEnabled();
		BlockPos pos = sign.getBlockPos();
		Section section = section(SectionPos.asLong(pos));
		section.seenFrame = frame;
		Entry entry = section.entries.get(pos.asLong());
		if (entry != null && entry.matches(sign, light, filtering)) {
			entry.owned = false;
			if (!section.dirty) {
				section.drawFrame = frame;
				// the up-close outline of coloured glowing text is drawn by vanilla, over the baked text
				skip = !((outlinedUpClose(sign.getFrontText()) || outlinedUpClose(sign.getBackText())) && upClose(pos, mc));
			}
			return;
		}
		if (entry == null) {
			section.entries.put(pos.asLong(), entry = new Entry(pos.immutable()));
		}
		entry.set(sign, light, filtering);
		entry.owned = false;
		section.dirty = true;
		capture = entry;
		Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
		shift.set((float) (camera.x - section.x), (float) (camera.y - section.y), (float) (camera.z - section.z));
	}

	private boolean outlinedUpClose(SignText text) {
		return text != null && text.hasGlowingText() && text.getColor().getTextColor() != BLACK;
	}

	// AbstractSignRenderer.isOutlineVisible for a colour other than black
	private boolean upClose(BlockPos pos, Minecraft mc) {
		if (scoping(mc)) {
			return true;
		}
		Entity camera = mc.getCameraEntity();
		return camera != null && camera.distanceToSqr(Vec3.atCenterOf(pos)) < OUTLINE_DISTANCE_SQ;
	}

	private boolean scoping(Minecraft mc) {
		return mc.player != null && mc.options.getCameraType().isFirstPerson() && mc.player.isScoping();
	}

	public boolean skip() {
		return skip;
	}

	public boolean capturing() {
		return capturing;
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
		Entry entry = section == null ? null : section.entries.remove(pos.asLong());
		if (entry != null) {
			entry.freeOutline();
			section.dirty = true;
			if (section.entries.isEmpty()) {
				section.free();
				SECTIONS.remove(key);
			}
		}
	}

	// after the block entities of the main pass: rebuild what changed, then draw every section left to its mesh
	public void draw() {
		if (!armed) {
			return;
		}
		armed = false;
		long now = frame++;
		if (SECTIONS.isEmpty() || !on()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
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
				// signs rendered this frame were drawn vanilla while the section was dirty
				if (section.seenFrame == now) {
					continue;
				}
			}
			if (section.drawFrame == now || section.owned && sodium != null && sodium.isBoxVisible(section.x, section.y, section.z, section.x + 16, section.y + 16, section.z + 16)) {
				draw.add(section);
			}
		}
		if (!draw.isEmpty()) {
			render(draw, camera, mc);
		}
	}

	private void validate() {
		ClientLevel now = Minecraft.getInstance().level;
		if (now != level || generation != TextMeshCache.generation) {
			for (Section section : SECTIONS.values()) {
				section.free();
			}
			SECTIONS.clear();
			level = now;
			generation = TextMeshCache.generation;
		}
	}

	private void rebuild(Section section) {
		section.free();
		Font font = Minecraft.getInstance().font;
		boolean owned = false;
		// a sign gone from the level without reaching remove() (seen when a player breaks one) must not leave its text behind
		section.entries.values().removeIf(entry -> !(level.getBlockEntity(entry.pos) instanceof SignBlockEntity sign) || sign.isRemoved());
		TextMeshCache.bypass(true);
		try {
			List<Line> lines = new ArrayList<>();
			List<Entry> outlined = new ArrayList<>();
			for (Entry entry : section.entries.values()) {
				owned |= entry.owned;
				lines.addAll(entry.lines);
				if (entry.owned) {
					// the outline coloured glowing text gets within 16 blocks: every such sign baked into its own buffers
					entry.outline = bake(font, entry.lines.stream().filter(line -> line.outline != 0 && !line.bakedOutline()).toList(), true);
					if (!entry.outline.isEmpty()) {
						outlined.add(entry);
					}
				}
			}
			section.meshes = bake(font, lines, false);
			section.outlined = outlined;
			section.owned = owned;
			section.dirty = false;
		} finally {
			TextMeshCache.bypass(false);
		}
	}

	private List<Mesh> bake(Font font, List<Line> lines, boolean outline) {
		if (lines.isEmpty()) {
			return List.of();
		}
		Map<RenderType, BufferBuilder> builders = new IdentityHashMap<>();
		List<ByteBufferBuilder> memory = new ArrayList<>();
		MultiBufferSource recorder = type -> builders.computeIfAbsent(type, t -> {
			ByteBufferBuilder bytes = new ByteBufferBuilder(outline ? 4096 : 1 << 16);
			memory.add(bytes);
			return new BufferBuilder(bytes, t.mode(), t.format());
		});
		try {
			for (Line line : lines) {
				if (outline || line.bakedOutline()) {
					font.drawInBatch8xOutline(line.text, line.x, line.y, line.color, line.outline, line.pose, recorder, line.light);
				} else {
					font.drawInBatch(line.text, line.x, line.y, line.color, false, line.pose, recorder, Font.DisplayMode.POLYGON_OFFSET, 0, line.light);
				}
			}
			List<Mesh> meshes = new ArrayList<>(builders.size());
			for (Map.Entry<RenderType, BufferBuilder> e : builders.entrySet()) {
				MeshData mesh = e.getValue().build();
				if (mesh != null) {
					VertexBuffer buffer = new VertexBuffer(BufferUsage.STATIC_WRITE);
					buffer.bind();
					buffer.upload(mesh);
					VertexBuffer.unbind();
					meshes.add(new Mesh(e.getKey(), buffer));
				}
			}
			return meshes;
		} finally {
			for (ByteBufferBuilder bytes : memory) {
				bytes.close();
			}
		}
	}

	private void render(List<Section> sections, Vec3 camera, Minecraft mc) {
		List<Draw> draws = new ArrayList<>();
		List<Draw> outlines = new ArrayList<>();
		boolean scoping = scoping(mc);
		Matrix4f view = RenderSystem.getModelViewMatrix();
		for (Section section : sections) {
			if (section.meshes.isEmpty()) {
				continue;
			}
			Matrix4f modelView = new Matrix4f(view).translate((float) (section.x - camera.x), (float) (section.y - camera.y), (float) (section.z - camera.z));
			for (Mesh mesh : section.meshes) {
				draws.add(new Draw(mesh, modelView));
			}
			if (section.outlined.isEmpty() || !scoping && camera.distanceToSqr(section.x + 8, section.y + 8, section.z + 8) > 30 * 30) {
				continue;
			}
			for (Entry entry : section.outlined) {
				double dx = entry.pos.getX() + 0.5 - camera.x, dy = entry.pos.getY() + 0.5 - camera.y, dz = entry.pos.getZ() + 0.5 - camera.z;
				if (!scoping && dx * dx + dy * dy + dz * dz >= OUTLINE_DISTANCE_SQ) {
					continue;
				}
				for (Mesh mesh : entry.outline) {
					outlines.add(new Draw(mesh, modelView));
				}
			}
		}
		submit(draws);
		submit(outlines);
	}

	private void submit(List<Draw> draws) {
		Map<RenderType, List<Draw>> byType = new IdentityHashMap<>();
		for (Draw draw : draws) {
			byType.computeIfAbsent(draw.mesh.type, t -> new ArrayList<>()).add(draw);
		}
		Matrix4f projection = RenderSystem.getProjectionMatrix();
		for (Map.Entry<RenderType, List<Draw>> e : byType.entrySet()) {
			RenderType type = e.getKey();
			type.setupRenderState();
			CompiledShaderProgram shader = RenderSystem.getShader();
			if (shader != null) {
				for (Draw draw : e.getValue()) {
					draw.mesh.buffer.bind();
					draw.mesh.buffer.drawWithShader(draw.modelView, projection, shader);
				}
				VertexBuffer.unbind();
			}
			type.clearRenderState();
		}
	}
}
