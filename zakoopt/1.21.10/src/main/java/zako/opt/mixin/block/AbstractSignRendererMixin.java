package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;
import zako.opt.ZakoOptConfig;

// every sign, every frame looked its model, material, render type and sprite up in maps keyed by WoodType / Material,
// records whose hashCode walks all fields through an ObjectMethods method handle; the answers never change per renderer
// (renderers are rebuilt on resource reload), so identity maps keyed by the same objects are enough
@Mixin(AbstractSignRenderer.class)
public abstract class AbstractSignRendererMixin {
	@Unique
	private final Map<BlockState, Model.Simple> zakoopt$models = new IdentityHashMap<>();
	@Unique
	private final Map<WoodType, Material> zakoopt$materials = new IdentityHashMap<>();
	@Unique
	private final Map<Material, RenderType> zakoopt$renderTypes = new IdentityHashMap<>();
	@Unique
	private final Map<Material, TextureAtlasSprite> zakoopt$sprites = new IdentityHashMap<>();

	@WrapOperation(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/AbstractSignRenderer;getSignModel(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/properties/WoodType;)Lnet/minecraft/client/model/Model$Simple;"))
	private Model.Simple zakoopt$model(AbstractSignRenderer renderer, BlockState state, WoodType wood, Operation<Model.Simple> original) {
		if (!ZakoOptConfig.signCache()) {
			return original.call(renderer, state, wood);
		}
		return zakoopt$models.computeIfAbsent(state, s -> original.call(renderer, s, wood));
	}

	@WrapOperation(method = "submitSign", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/AbstractSignRenderer;getSignMaterial(Lnet/minecraft/world/level/block/state/properties/WoodType;)Lnet/minecraft/client/resources/model/Material;"))
	private Material zakoopt$material(AbstractSignRenderer renderer, WoodType wood, Operation<Material> original) {
		if (!ZakoOptConfig.signCache()) {
			return original.call(renderer, wood);
		}
		return zakoopt$materials.computeIfAbsent(wood, w -> original.call(renderer, w));
	}

	@WrapOperation(method = "submitSign", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/Material;renderType(Ljava/util/function/Function;)Lnet/minecraft/client/renderer/RenderType;"))
	private RenderType zakoopt$renderType(Material material, Function<?, ?> factory, Operation<RenderType> original) {
		if (!ZakoOptConfig.signCache()) {
			return original.call(material, factory);
		}
		return zakoopt$renderTypes.computeIfAbsent(material, m -> original.call(m, factory));
	}

	@WrapOperation(method = "submitSign", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/MaterialSet;get(Lnet/minecraft/client/resources/model/Material;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"))
	private TextureAtlasSprite zakoopt$sprite(MaterialSet materials, Material material, Operation<TextureAtlasSprite> original) {
		if (!ZakoOptConfig.signCache()) {
			return original.call(materials, material);
		}
		return zakoopt$sprites.computeIfAbsent(material, m -> original.call(materials, m));
	}
}
