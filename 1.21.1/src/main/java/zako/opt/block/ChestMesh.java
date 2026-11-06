package zako.opt.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import lombok.experimental.UtilityClass;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.joml.Vector3f;
import zako.opt.mixin.block.ChestRendererXmasAccessor;
import zako.opt.mixin.entity.ModelPartCubeAccessor;

// shut chests baked into Sodium's chunk mesh on the builder threads, from the block atlas copy of the chest textures
@UtilityClass
public class ChestMesh {
	// entity textures have no mipmaps; entityCutout cuts at half alpha
	private final Material CUTOUT = new Material(DefaultTerrainRenderPasses.CUTOUT, AlphaCutoffParameter.HALF, false);

	private record Models(Object owner, boolean xmas, ModelPart single, ModelPart left, ModelPart right) {
	}

	// own copies at rest: the renderer's models get their lid angle set on the render thread meanwhile
	private volatile Models models;

	public boolean bake(BlockEntity blockEntity, Object renderer, ChunkBuildBuffers buffers, LevelSlice level) {
		Models m = models;
		if (m == null || m.owner != renderer) {
			models = m = new Models(renderer, ((ChestRendererXmasAccessor) renderer).zakoopt$xmas(), layer(ModelLayers.CHEST), layer(ModelLayers.DOUBLE_CHEST_LEFT), layer(ModelLayers.DOUBLE_CHEST_RIGHT));
		}
		BlockState state = blockEntity.getBlockState();
		ChestType chest = state.hasProperty(ChestBlock.TYPE) ? state.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
		TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(Sheets.chooseMaterial(blockEntity, chest, m.xmas).texture());
		if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
			return false;
		}

		BlockPos pos = blockEntity.getBlockPos();
		int light = LevelRenderer.getLightColor(level, pos);
		PoseStack poseStack = new PoseStack();
		poseStack.translate((pos.getX() & 15) + 0.5F, (pos.getY() & 15) + 0.5F, (pos.getZ() & 15) + 0.5F);
		poseStack.mulPose(Axis.YP.rotationDegrees(-state.getValue(ChestBlock.FACING).toYRot()));
		poseStack.translate(-0.5F, -0.5F, -0.5F);
		VertexConsumer out = buffers.get(CUTOUT).asFallbackVertexConsumer(CUTOUT, null);
		Vector3f normal = new Vector3f();
		Vector3f p = new Vector3f();
		(chest == ChestType.LEFT ? m.left : chest == ChestType.RIGHT ? m.right : m.single).visit(poseStack, (pose, name, index, cube) -> {
			for (ModelPart.Polygon polygon : ((ModelPartCubeAccessor) cube).zakoopt$polygons()) {
				pose.transformNormal(polygon.normal, normal);
				// the chunk shader has no entity lighting: shaded like a block face instead
				float shade = Math.min(normal.x * normal.x * 0.6F + normal.z * normal.z * 0.8F + normal.y * normal.y * (normal.y > 0 ? 1.0F : 0.5F), 1.0F);
				for (ModelPart.Vertex v : polygon.vertices) {
					pose.pose().transformPosition(v.pos.x() / 16.0F, v.pos.y() / 16.0F, v.pos.z() / 16.0F, p);
					out.addVertex(p.x, p.y, p.z).setColor(shade, shade, shade, 1.0F).setUv(sprite.getU(v.u), sprite.getV(v.v)).setLight(light).setNormal(normal.x, normal.y, normal.z);
				}
			}
		});
		return true;
	}

	public void dirty(BlockPos pos) {
		Minecraft.getInstance().levelRenderer.setSectionDirty(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getY()), SectionPos.blockToSectionCoord(pos.getZ()));
	}

	private ModelPart layer(ModelLayerLocation layer) {
		return Minecraft.getInstance().getEntityModels().bakeLayer(layer);
	}
}
