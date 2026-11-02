package zako.opt.mixin.block;

import zako.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import zako.opt.text.StableText;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

// every sign, every frame looked its model, material, render type and sprite up in maps keyed by WoodType / Material,
// records whose hashCode walks all fields; the answers never change per renderer (renderers are rebuilt on resource reload).
// HangingSignRenderer inherits renderSign and renderSignText, so this covers both but their own model lookup
@Mixin(SignRenderer.class)
public abstract class SignRendererMixin {
	@Unique
	private final Map<WoodType, Object> zakoopt$models = new IdentityHashMap<>();
	@Unique
	private final Map<WoodType, Material> zakoopt$materials = new IdentityHashMap<>();
	@Unique
	private final Map<Material, RenderType> zakoopt$renderTypes = new IdentityHashMap<>();
	@Unique
	private final Map<Material, TextureAtlasSprite> zakoopt$sprites = new IdentityHashMap<>();
	@Unique
	private final Map<FormattedCharSequence, Integer> zakoopt$widths = new IdentityHashMap<>();

	@WrapOperation(method = "render(Lnet/minecraft/world/level/block/entity/SignBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
			at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
	private Object zakoopt$model(Map<WoodType, Object> models, Object wood, Operation<Object> original) {
		if (!Features.SIGN_CACHE.on) {
			return original.call(models, wood);
		}
		return zakoopt$models.computeIfAbsent((WoodType) wood, w -> original.call(models, w));
	}

	@WrapOperation(method = "renderSign", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/SignRenderer;getSignMaterial(Lnet/minecraft/world/level/block/state/properties/WoodType;)Lnet/minecraft/client/resources/model/Material;"))
	private Material zakoopt$material(SignRenderer renderer, WoodType wood, Operation<Material> original) {
		if (!Features.SIGN_CACHE.on) {
			return original.call(renderer, wood);
		}
		return zakoopt$materials.computeIfAbsent(wood, w -> original.call(renderer, w));
	}

	// Material.buffer is sprite().wrap(buffers.getBuffer(renderType(factory))); sprite and render type are fixed per material
	@WrapOperation(method = "renderSign", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/Material;buffer(Lnet/minecraft/client/renderer/MultiBufferSource;Ljava/util/function/Function;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
	private VertexConsumer zakoopt$buffer(Material material, MultiBufferSource buffers, Function<ResourceLocation, RenderType> factory, Operation<VertexConsumer> original) {
		if (!Features.SIGN_CACHE.on) {
			return original.call(material, buffers, factory);
		}
		RenderType type = zakoopt$renderTypes.computeIfAbsent(material, m -> m.renderType(factory));
		return zakoopt$sprites.computeIfAbsent(material, Material::sprite).wrap(buffers.getBuffer(type));
	}

	// a sign keeps its split lines until its text changes, so the text mesh cache may hold on to them
	@ModifyArg(method = "renderSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/SignText;getRenderMessages(ZLjava/util/function/Function;)[Lnet/minecraft/util/FormattedCharSequence;"), index = 1)
	private Function<Component, FormattedCharSequence> zakoopt$stableLines(Function<Component, FormattedCharSequence> split) {
		return Features.PREPARED_TEXT_CACHE.on ? component -> StableText.mark(split.apply(component)) : split;
	}

	@WrapOperation(method = "renderSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;width(Lnet/minecraft/util/FormattedCharSequence;)I"))
	private int zakoopt$cachedWidth(Font font, FormattedCharSequence line, Operation<Integer> original) {
		if (!Features.PREPARED_TEXT_CACHE.on) {
			return original.call(font, line);
		}
		Integer width = zakoopt$widths.get(line);
		if (width == null) {
			// ponytail: wholesale clear instead of LRU, the visible signs refill it within a frame
			if (zakoopt$widths.size() >= 4096) {
				zakoopt$widths.clear();
			}
			width = original.call(font, line);
			zakoopt$widths.put(line, width);
		}
		return width;
	}
}
