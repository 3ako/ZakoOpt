package zako.opt.block;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageCouplingRenderer;
import com.simibubi.create.content.trains.entity.Train;
import lombok.experimental.UtilityClass;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import zako.opt.gl.RawVertices;
import zako.opt.mixin.block.CreateBogeyAccessor;
import zako.opt.mixin.block.CreateFrustumAccessor;

import java.nio.ByteBuffer;
import java.util.List;

// a coupling's cable is one model per quarter block, each transformed, shaded and written vertex by vertex every frame; those
// models differ only by a shift along the cable, so the first is drawn the original way and the rest are its copies, shifted
@UtilityClass
public class TrainCouplings {
	private final ByteBufferBuilder SCRATCH = new ByteBufferBuilder(2048);
	private final int RUN = 16384;
	// Create's classes no longer match ours: the original draws from then on
	private boolean off;
	private final Vector3f step = new Vector3f();

	// false: draw it the original way
	public boolean render(PoseStack ms, MultiBufferSource buffer, Vec3 camera) {
		if (off) {
			return false;
		}
		try {
			RenderType type = RenderType.solid();
			VertexFormat format = type.format();
			VertexConsumer vb = buffer.getBuffer(type);
			if (!RawVertices.accepts(vb, format)) {
				return false;
			}
			BlockState air = Blocks.AIR.defaultBlockState();
			float partialTicks = AnimationTickHolder.getPartialTicks();
			Level level = Minecraft.getInstance().level;
			Frustum frustum = ((CreateFrustumAccessor) Minecraft.getInstance().levelRenderer).zakoopt$frustum();
			for (Train train : CreateClient.RAILWAYS.trains.values()) {
				List<Carriage> carriages = train.carriages;
				for (int i = 0; i < carriages.size() - 1; i++) {
					Carriage carriage = carriages.get(i);
					CarriageContraptionEntity entity = carriage.getDimensional(level).entity.get();
					if (entity == null) {
						continue;
					}
					CarriageBogey bogey1 = carriage.trailingBogey();
					CarriageBogey bogey2 = carriages.get(i + 1).leadingBogey();
					Vec3 anchor = bogey1.couplingAnchors.getSecond();
					Vec3 anchor2 = bogey2.couplingAnchors.getFirst();
					if (anchor == null || anchor2 == null || !anchor.closerThan(camera, 64)) {
						continue;
					}
					if (frustum != null && !frustum.isVisible(new AABB(anchor, anchor2).inflate(1.5))) {
						continue;
					}
					// both ends take the first carriage's light, as the original reads the same entity twice
					int light = CarriageCouplingRenderer.getPackedLightCoords(entity, partialTicks);
					double diffX = anchor2.x - anchor.x;
					double diffY = anchor2.y - anchor.y;
					double diffZ = anchor2.z - anchor.z;
					float yRot = AngleHelper.deg(Mth.atan2(diffZ, diffX)) + 90;
					float xRot = AngleHelper.deg(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ)));

					ms.pushPose();
					ms.translate(anchor.x - camera.x, anchor.y - camera.y, anchor.z - camera.z);
					CachedBuffers.partial(AllPartialModels.TRAIN_COUPLING_HEAD, air)
						.rotateYDegrees(-yRot)
						.rotateXDegrees(xRot)
						.light(light)
						.renderInto(ms, vb);

					float margin = 3 / 16f;
					double distance = train.carriageSpacing.get(i) - 2 * margin
						- ((CreateBogeyAccessor) bogey1).zakoopt$type().getConnectorAnchorOffset(bogey1.isUpsideDown()).z - ((CreateBogeyAccessor) bogey2).zakoopt$type().getConnectorAnchorOffset(bogey2.isUpsideDown()).z;
					int segments = (int) Math.round(distance * 4);
					if (segments > 0) {
						double stretch = ((anchor2.distanceTo(anchor) - 2 * margin) * 4) / segments;
						SuperByteBuffer cable = CachedBuffers.partial(AllPartialModels.TRAIN_COUPLING_CABLE, air)
							.rotateYDegrees(-yRot + 180)
							.rotateXDegrees(-xRot)
							.translate(0, 0, margin + 2 / 16f)
							.scale(1, 1, (float) stretch)
							.light(light);
						BufferBuilder first = new BufferBuilder(SCRATCH, type.mode(), format);
						cable.renderInto(ms, first);
						new Matrix4f(ms.last().pose()).rotateY(Mth.DEG_TO_RAD * (-yRot + 180)).rotateX(Mth.DEG_TO_RAD * -xRot)
							.transformDirection(0, 0, (float) stretch / 4, step);
						try (MeshData mesh = first.build()) {
							if (mesh != null) {
								repeat(vb, mesh.vertexBuffer(), mesh.drawState().vertexCount(), segments, format);
							}
						}
					}
					ms.popPose();

					ms.pushPose();
					ms.translate(anchor2.x - camera.x, anchor2.y - camera.y, anchor2.z - camera.z);
					CachedBuffers.partial(AllPartialModels.TRAIN_COUPLING_HEAD, air)
						.rotateYDegrees(-yRot + 180)
						.rotateXDegrees(-xRot)
						.light(light)
						.renderInto(ms, vb);
					ms.popPose();
				}
			}
			return true;
		} catch (RuntimeException | LinkageError t) {
			off = true;
			throw t;
		}
	}

	private void repeat(VertexConsumer target, ByteBuffer model, int count, int segments, VertexFormat format) {
		int stride = format.getVertexSize();
		int size = count * stride;
		// runs that fit the 64 KB MemoryStack Sodium copies through
		int perRun = Math.max(1, RUN / size);
		long src = MemoryUtil.memAddress(model);
		for (int done = 0; done < segments; done += perRun) {
			int n = Math.min(perRun, segments - done);
			long dst = RawVertices.scratch((long) n * size);
			for (int j = 0; j < n; j++) {
				long to = dst + (long) j * size;
				MemoryUtil.memCopy(src, to, size);
				float dx = step.x * (done + j), dy = step.y * (done + j), dz = step.z * (done + j);
				for (long v = to, end = to + size; v < end; v += stride) {
					MemoryUtil.memPutFloat(v, MemoryUtil.memGetFloat(v) + dx);
					MemoryUtil.memPutFloat(v + 4, MemoryUtil.memGetFloat(v + 4) + dy);
					MemoryUtil.memPutFloat(v + 8, MemoryUtil.memGetFloat(v + 8) + dz);
				}
			}
			try (MemoryStack stack = MemoryStack.stackPush()) {
				RawVertices.push(target, stack, dst, n * count, format);
			}
		}
	}
}
