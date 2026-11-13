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
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.joml.Vector3f;
import org.slf4j.LoggerFactory;
import zako.opt.feature.Features;
import zako.opt.mixin.block.SignRendererInvoker;
import zako.opt.mixin.entity.ModelPartCubeAccessor;
import zako.opt.text.SignTextMesh;

import java.util.Map;
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

	// own copies: the renderer's models get their part visibility set on the render thread meanwhile; the layers are code-defined
	private final Map<WoodType, ModelPart> SIGNS = new ConcurrentHashMap<>();
	private final Map<WoodType, ModelPart> HANGING = new ConcurrentHashMap<>();

	public boolean bake(BlockEntity blockEntity, Object renderer, ChunkBuildBuffers buffers, LevelSlice level) {
		if (!(blockEntity instanceof SignBlockEntity sign) || renderer == null || renderer.getClass() != SignRenderer.class && renderer.getClass() != HangingSignRenderer.class
				|| !Features.SIGN_CACHE.on || Features.shaders() || FAILED.get() || BREAKING.contains(blockEntity.getBlockPos().asLong())) {
			return false;
		}
		try {
			if (!emit(sign, (SignRenderer) renderer, buffers, level)) {
				return false;
			}
		} catch (RuntimeException e) {
			if (!FAILED.getAndSet(true)) {
				LoggerFactory.getLogger("zakoopt").error("Baking signs into the chunk mesh failed, drawing them the vanilla way from now on", e);
			}
			return false;
		}
		SignTextMesh.queue(sign, (SignRenderer) renderer);
		return true;
	}

	private boolean emit(SignBlockEntity sign, SignRenderer renderer, ChunkBuildBuffers buffers, LevelSlice level) {
		BlockState state = sign.getBlockState();
		if (!(state.getBlock() instanceof SignBlock block)) {
			return false;
		}
		boolean hanging = renderer instanceof HangingSignRenderer;
		WoodType wood = SignBlock.getWoodType(block);
		TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
				.apply((hanging ? Sheets.getHangingSignMaterial(wood) : Sheets.getSignMaterial(wood)).texture());
		if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
			return false;
		}
		ModelPart root = (hanging ? HANGING : SIGNS).computeIfAbsent(wood, w -> Minecraft.getInstance().getEntityModels()
				.bakeLayer(hanging ? ModelLayers.createHangingSignModelName(w) : ModelLayers.createSignModelName(w)));

		BlockPos pos = sign.getBlockPos();
		int light = LevelRenderer.getLightColor(level, pos);
		PoseStack poseStack = new PoseStack();
		poseStack.translate(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
		((SignRendererInvoker) renderer).zakoopt$translate(poseStack, -block.getYRotationDegrees(state), state);
		float scale = renderer.getSignModelRenderScale();
		poseStack.scale(scale, -scale, -scale);
		VertexConsumer out = buffers.get(CUTOUT).asFallbackVertexConsumer(CUTOUT, null);
		Vector3f normal = new Vector3f();
		Vector3f[] p = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
		float[] u = new float[4], v = new float[4];
		ModelPart.Visitor visitor = (pose, name, index, cube) -> {
			for (ModelPart.Polygon polygon : ((ModelPartCubeAccessor) cube).zakoopt$polygons()) {
				pose.transformNormal(polygon.normal, normal);
				float shade = lit(normal.x, normal.y, normal.z), back = lit(-normal.x, -normal.y, -normal.z);
				ModelPart.Vertex[] vertices = polygon.vertices;
				for (int i = 0; i < 4; i++) {
					ModelPart.Vertex vertex = vertices[i];
					pose.pose().transformPosition(vertex.pos.x() / 16.0F, vertex.pos.y() / 16.0F, vertex.pos.z() / 16.0F, p[i]);
					u[i] = sprite.getU(vertex.u);
					v[i] = sprite.getV(vertex.v);
				}
				for (int i = 0; i < 4; i++) {
					out.addVertex(p[i].x, p[i].y, p[i].z).setColor(shade, shade, shade, 1.0F).setUv(u[i], v[i]).setLight(light).setNormal(normal.x, normal.y, normal.z);
				}
				// sign models draw with no culling (hanging sign chains are flat): each face also goes in the other winding
				for (int i = 3; i >= 0; i--) {
					out.addVertex(p[i].x, p[i].y, p[i].z).setColor(back, back, back, 1.0F).setUv(u[i], v[i]).setLight(light).setNormal(-normal.x, -normal.y, -normal.z);
				}
			}
		};
		// the parts the renderer would leave visible for this state, see SignRenderer.render and HangingSignModel.evaluateVisibleParts
		if (hanging) {
			boolean ceiling = block instanceof CeilingHangingSignBlock;
			boolean attached = ceiling && state.getValue(BlockStateProperties.ATTACHED);
			root.getChild("board").visit(poseStack, visitor);
			if (!ceiling) {
				root.getChild("plank").visit(poseStack, visitor);
			}
			root.getChild(attached ? "vChains" : "normalChains").visit(poseStack, visitor);
		} else {
			root.getChild("sign").visit(poseStack, visitor);
			if (block instanceof StandingSignBlock) {
				root.getChild("stick").visit(poseStack, visitor);
			}
		}
		return true;
	}

	// the entity shader's level lighting (two fixed lights, 0.4 ambient), as signs get it as block entities
	private float lit(float x, float y, float z) {
		float a = Math.max(0.0F, (0.2F * x + y - 0.7F * z) * 0.8084F), b = Math.max(0.0F, (-0.2F * x + y + 0.7F * z) * 0.8084F);
		return Math.min(1.0F, 0.4F + 0.6F * (a + b));
	}
}
