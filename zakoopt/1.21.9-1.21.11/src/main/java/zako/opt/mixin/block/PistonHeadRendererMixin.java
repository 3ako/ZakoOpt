package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.blockentity.PistonHeadRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;
import zako.opt.block.TickCached;

@Mixin(PistonHeadRenderer.class)
public class PistonHeadRendererMixin {
	// a moving piston block entity lives 2 ticks at one position; look the biome up once
	@WrapOperation(method = "extractRenderState(Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/PistonHeadRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBiome(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;"))
	@SuppressWarnings("unchecked")
	private Holder<Biome> zakoopt$cachedBiome(Level level, BlockPos pos, Operation<Holder<Biome>> original, PistonMovingBlockEntity blockEntity) {
		if (!ZakoOptConfig.lookupCaches()) {
			return original.call(level, pos);
		}
		TickCached cache = (TickCached) blockEntity;
		if (cache.zakoopt$cached() == null) {
			cache.zakoopt$setCached(original.call(level, pos), 0);
		}
		return (Holder<Biome>) cache.zakoopt$cached();
	}
}
