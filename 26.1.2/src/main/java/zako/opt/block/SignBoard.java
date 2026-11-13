package zako.opt.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.experimental.UtilityClass;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.StandingSignRenderer;
import net.minecraft.client.renderer.blockentity.state.HangingSignRenderState;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.blockentity.state.StandingSignRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.HangingSignBlock;
import net.minecraft.world.level.block.PlainSignBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.slf4j.LoggerFactory;
import zako.opt.feature.Features;
import zako.opt.mixin.block.AbstractSignRendererInvoker;
import zako.opt.text.SignTextMesh;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

// sign boards baked into Sodium's chunk mesh on the builder threads; the text goes to SignTextMesh, the block entity is left out
@UtilityClass
public class SignBoard {
	private final Material CUTOUT = new Material(DefaultTerrainRenderPasses.CUTOUT, AlphaCutoffParameter.HALF, false);
	private final AtomicBoolean FAILED = new AtomicBoolean();
	// crack overlays are drawn by the sign renderer: a sign being broken stays a block entity
	public final Set<Long> BREAKING = ConcurrentHashMap.newKeySet();

	public boolean bake(BlockEntity blockEntity, Object renderer, ChunkBuildBuffers buffers, LevelSlice level) {
		if (!(blockEntity instanceof SignBlockEntity sign) || !(renderer instanceof AbstractSignRenderer<?> signRenderer) || !Features.SIGN_CACHE.on
				|| Features.shaders() || FAILED.get() || BREAKING.contains(blockEntity.getBlockPos().asLong())) {
			return false;
		}
		try {
			if (!emit(sign, renderer, buffers, level)) {
				return false;
			}
		} catch (RuntimeException e) {
			if (!FAILED.getAndSet(true)) {
				LoggerFactory.getLogger("zakoopt").error("Baking signs into the chunk mesh failed, drawing them the vanilla way from now on", e);
			}
			return false;
		}
		SignTextMesh.queue(sign, signRenderer);
		return true;
	}

	// the same state the renderer extracts, minus light and text: built here so the level is not read off the render thread
	private SignRenderState state(Object renderer, BlockState blockState) {
		if (renderer instanceof StandingSignRenderer) {
			StandingSignRenderState state = new StandingSignRenderState();
			state.attachmentType = PlainSignBlock.getAttachmentPoint(blockState);
			state.transformations = blockState.getBlock() instanceof WallSignBlock
					? StandingSignRenderer.TRANSFORMATIONS.wallTransformation(blockState.getValue(WallSignBlock.FACING))
					: StandingSignRenderer.TRANSFORMATIONS.freeTransformations(blockState.getValue(StandingSignBlock.ROTATION));
			return state;
		}
		if (renderer instanceof HangingSignRenderer) {
			HangingSignRenderState state = new HangingSignRenderState();
			state.attachmentType = HangingSignBlock.getAttachmentPoint(blockState);
			state.transformations = blockState.getBlock() instanceof WallHangingSignBlock
					? HangingSignRenderer.TRANSFORMATIONS.wallTransformation(blockState.getValue(WallHangingSignBlock.FACING))
					: HangingSignRenderer.TRANSFORMATIONS.freeTransformations(blockState.getValue(CeilingHangingSignBlock.ROTATION));
			return state;
		}
		return null;
	}

	private boolean emit(SignBlockEntity sign, Object renderer, ChunkBuildBuffers buffers, LevelSlice level) {
		BlockState blockState = sign.getBlockState();
		SignRenderState state = state(renderer, blockState);
		if (state == null || !(blockState.getBlock() instanceof SignBlock)) {
			return false;
		}
		state.woodType = SignBlock.getWoodType(blockState.getBlock());
		AbstractSignRendererInvoker invoker = (AbstractSignRendererInvoker) renderer;
		TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
		TextureAtlasSprite sprite = atlas.getSprite(invoker.zakoopt$sprite(state.woodType).texture());
		if (sprite == atlas.missingSprite()) {
			return false;
		}
		ModelPart root = invoker.zakoopt$model(state).root();
		BlockPos pos = sign.getBlockPos();
		int light = LightCoordsUtil.pack(level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(LightLayer.SKY, pos));
		PoseStack poseStack = new PoseStack();
		poseStack.translate(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
		poseStack.mulPose(state.transformations.body());
		VertexConsumer out = buffers.get(CUTOUT).asFallbackVertexConsumer(CUTOUT, null);
		Vector3f normal = new Vector3f();
		Vector3f[] p = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
		float[] u = new float[4], v = new float[4];
		root.visit(poseStack, (pose, name, index, cube) -> {
			for (ModelPart.Polygon polygon : cube.polygons) {
				pose.transformNormal(polygon.normal(), normal);
				float shade = lit(normal.x, normal.y, normal.z), back = lit(-normal.x, -normal.y, -normal.z);
				int color = ARGB.colorFromFloat(1.0F, shade, shade, shade), backColor = ARGB.colorFromFloat(1.0F, back, back, back);
				ModelPart.Vertex[] vertices = polygon.vertices();
				for (int i = 0; i < 4; i++) {
					ModelPart.Vertex vertex = vertices[i];
					pose.pose().transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), p[i]);
					u[i] = sprite.getU(vertex.u());
					v[i] = sprite.getV(vertex.v());
				}
				for (int i = 0; i < 4; i++) {
					out.addVertex(p[i].x, p[i].y, p[i].z).setColor(color).setUv(u[i], v[i]).setLight(light).setNormal(normal.x, normal.y, normal.z);
				}
				// sign models draw with no culling (hanging sign chains are flat): each face also goes in the other winding
				for (int i = 3; i >= 0; i--) {
					out.addVertex(p[i].x, p[i].y, p[i].z).setColor(backColor).setUv(u[i], v[i]).setLight(light).setNormal(-normal.x, -normal.y, -normal.z);
				}
			}
		});
		return true;
	}

	// the entity shader's level lighting (two fixed lights, 0.4 ambient), as signs get it as block entities
	private float lit(float x, float y, float z) {
		float a = Math.max(0.0F, (0.2F * x + y - 0.7F * z) * 0.8084F), b = Math.max(0.0F, (-0.2F * x + y + 0.7F * z) * 0.8084F);
		return Math.min(1.0F, 0.4F + 0.6F * (a + b));
	}
}
