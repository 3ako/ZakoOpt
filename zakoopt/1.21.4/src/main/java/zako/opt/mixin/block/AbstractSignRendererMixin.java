package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

// every sign, every frame looked its model, material, render type and sprite up in maps keyed by WoodType / Material,
// records whose hashCode walks all fields; the answers never change per renderer (renderers are rebuilt on resource reload)
@Mixin(AbstractSignRenderer.class)
public abstract class AbstractSignRendererMixin {
	@Unique
	private final Map<BlockState, Model> zakoopt$models = new IdentityHashMap<>();
	@Unique
	private final Map<WoodType, Material> zakoopt$materials = new IdentityHashMap<>();
	@Unique
	private final Map<Material, RenderType> zakoopt$renderTypes = new IdentityHashMap<>();
	@Unique
	private final Map<Material, TextureAtlasSprite> zakoopt$sprites = new IdentityHashMap<>();

	@WrapOperation(method = "render(Lnet/minecraft/world/level/block/entity/SignBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/AbstractSignRenderer;getSignModel(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/properties/WoodType;)Lnet/minecraft/client/model/Model;"))
	private Model zakoopt$model(AbstractSignRenderer renderer, BlockState state, WoodType wood, Operation<Model> original) {
		if (!ZakoOptConfig.signCache()) {
			return original.call(renderer, state, wood);
		}
		return zakoopt$models.computeIfAbsent(state, s -> original.call(renderer, s, wood));
	}

	@WrapOperation(method = "renderSign", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/AbstractSignRenderer;getSignMaterial(Lnet/minecraft/world/level/block/state/properties/WoodType;)Lnet/minecraft/client/resources/model/Material;"))
	private Material zakoopt$material(AbstractSignRenderer renderer, WoodType wood, Operation<Material> original) {
		if (!ZakoOptConfig.signCache()) {
			return original.call(renderer, wood);
		}
		return zakoopt$materials.computeIfAbsent(wood, w -> original.call(renderer, w));
	}

	// Material.buffer is sprite().wrap(buffers.getBuffer(renderType(factory))); sprite and render type are fixed per material
	@WrapOperation(method = "renderSign", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/Material;buffer(Lnet/minecraft/client/renderer/MultiBufferSource;Ljava/util/function/Function;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
	private VertexConsumer zakoopt$buffer(Material material, MultiBufferSource buffers, Function<ResourceLocation, RenderType> factory, Operation<VertexConsumer> original) {
		if (!ZakoOptConfig.signCache()) {
			return original.call(material, buffers, factory);
		}
		RenderType type = zakoopt$renderTypes.computeIfAbsent(material, m -> m.renderType(factory));
		return zakoopt$sprites.computeIfAbsent(material, Material::sprite).wrap(buffers.getBuffer(type));
	}
}
