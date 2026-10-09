package zako.opt.mixin.block;

import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.model.light.LightMode;
import net.caffeinemc.mods.sodium.client.render.frapi.render.NonTerrainBlockRenderContext;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.ZakoOptConfig;

@Mixin(value = NonTerrainBlockRenderContext.class, remap = false)
public class NonTerrainBlockRenderContextMixin {
	@Unique
	private boolean zakoopt$flatLight;

	// piston-moved blocks are rendered this way every frame for 2 ticks; smooth AO there is most of their cost
	@Inject(method = "renderModel", at = @At("HEAD"))
	private void zakoopt$detectMoving(CallbackInfo ci, @Local(argsOnly = true) BlockAndTintGetter level) {
		zakoopt$flatLight = level instanceof MovingBlockRenderState && ZakoOptConfig.movingBlockFlatLight();
	}

	@ModifyVariable(method = "shadeQuad", at = @At("HEAD"), argsOnly = true)
	private LightMode zakoopt$flat(LightMode mode) {
		return zakoopt$flatLight ? LightMode.FLAT : mode;
	}
}
