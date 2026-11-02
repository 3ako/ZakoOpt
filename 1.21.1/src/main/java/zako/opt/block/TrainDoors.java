package zako.opt.block;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import lombok.experimental.UtilityClass;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import zako.opt.gl.RawVertices;
import zako.opt.mixin.block.CreateDoorAccessor;
import zako.opt.mixin.block.CreateFrustumAccessor;

import java.util.ArrayList;
import java.util.List;

// a contraption draws every door vertex by vertex each frame, seen or not; doors alike in state, opening, light and rotation
// differ only by a shift, so the first is drawn by Create's renderer and the rest, on later frames too, are its copies, shifted
@UtilityClass
public class TrainDoors {
	private final int MAX_BYTES = 16384;
	private final int MAX_PIECES = 256;
	private final double RADIUS = 2;
	private final ByteBufferBuilder SCRATCH = new ByteBufferBuilder(4096);
	// ponytail: linear scan and wholesale clear at the cap, a map on the state if many different doors ever show up
	private final List<Piece> pieces = new ArrayList<>();
	private final PoseStack origin = new PoseStack();
	private final Matrix3f rotation = new Matrix3f();
	private BakedModel models;
	private DimensionSpecialEffects shading;
	private boolean drawing;
	private boolean off;

	// false: draw it the original way
	public boolean render(SlidingDoorRenderer renderer, SlidingDoorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		if (off || drawing || !(be.getLevel() instanceof VirtualRenderWorld)) {
			return false;
		}
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc.screen instanceof PonderUI) {
				return false;
			}
			CreateDoorAccessor door = (CreateDoorAccessor) be;
			BlockState state = be.getBlockState();
			if (!door.zakoopt$shouldRenderSpecial(state)) {
				return true;
			}
			RenderType type = RenderType.cutoutMipped();
			VertexFormat format = type.format();
			VertexConsumer vb = buffer.getBuffer(type);
			if (!RawVertices.accepts(vb, format)) {
				return false;
			}
			Matrix4f pose = ms.last().pose();
			Frustum frustum = ((CreateFrustumAccessor) mc.levelRenderer).zakoopt$frustum();
			if (frustum != null) {
				Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
				double x = pose.m00() * .5f + pose.m10() + pose.m20() * .5f + pose.m30() + camera.x;
				double y = pose.m01() * .5f + pose.m11() + pose.m21() * .5f + pose.m31() + camera.y;
				double z = pose.m02() * .5f + pose.m12() + pose.m22() * .5f + pose.m32() + camera.z;
				if (!frustum.isVisible(new AABB(x - RADIUS, y - RADIUS, z - RADIUS, x + RADIUS, y + RADIUS, z + RADIUS))) {
					return true;
				}
			}
			// a resource reload moves the atlas, the dimension changes the shade; curves and openings keep adding rotations and values
			BakedModel currentModels = mc.getModelManager().getMissingModel();
			DimensionSpecialEffects currentShading = mc.level.effects();
			if (pieces.size() >= MAX_PIECES || models != currentModels || shading != currentShading) {
				models = currentModels;
				shading = currentShading;
				pieces.forEach(piece -> piece.mesh.close());
				pieces.clear();
			}
			float value = door.zakoopt$animation().getValue(partialTicks);
			pose.get3x3(rotation);
			Piece piece = find(state, value, light);
			if (piece == null) {
				piece = draw(renderer, be, partialTicks, ms, light, overlay, type, format, state, value);
				if (piece == null) {
					return true;
				}
			}
			int count = piece.mesh.drawState().vertexCount();
			int stride = format.getVertexSize();
			int size = count * stride;
			if (size > MAX_BYTES) {
				off = true;
				return false;
			}
			long dst = RawVertices.scratch(size);
			MemoryUtil.memCopy(MemoryUtil.memAddress(piece.mesh.vertexBuffer()), dst, size);
			TrainCouplings.shift(dst, size, stride, pose.m30(), pose.m31(), pose.m32());
			try (MemoryStack stack = MemoryStack.stackPush()) {
				RawVertices.push(vb, stack, dst, count, format);
			}
			return true;
		} catch (RuntimeException | LinkageError t) {
			off = true;
			return false;
		}
	}

	private Piece find(BlockState state, float value, int light) {
		for (int i = 0; i < pieces.size(); i++) {
			Piece piece = pieces.get(i);
			if (piece.state == state && piece.value == value && piece.light == light && piece.rotation.equals(rotation)) {
				return piece;
			}
		}
		return null;
	}

	private Piece draw(SlidingDoorRenderer renderer, SlidingDoorBlockEntity be, float partialTicks, PoseStack ms, int light, int overlay, RenderType type, VertexFormat format, BlockState state, float value) {
		origin.last().pose().set(ms.last().pose()).setTranslation(0, 0, 0);
		origin.last().normal().set(ms.last().normal());
		BufferBuilder builder = new BufferBuilder(SCRATCH, type.mode(), format);
		drawing = true;
		try {
			renderer.render(be, partialTicks, origin, ignored -> builder, light, overlay);
		} finally {
			drawing = false;
		}
		MeshData mesh = builder.build();
		if (mesh == null) {
			return null;
		}
		Piece piece = new Piece(state, value, light, new Matrix3f(rotation), mesh);
		pieces.add(piece);
		return piece;
	}

	private record Piece(BlockState state, float value, int light, Matrix3f rotation, MeshData mesh) {
	}
}
