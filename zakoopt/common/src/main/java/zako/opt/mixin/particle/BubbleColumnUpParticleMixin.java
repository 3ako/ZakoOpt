package zako.opt.mixin.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BubbleColumnUpParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;

import java.lang.ref.WeakReference;

@Mixin(BubbleColumnUpParticle.class)
public class BubbleColumnUpParticleMixin {
	@Unique
	private static final Long2ObjectOpenHashMap<FluidState> zakoopt$fluids = new Long2ObjectOpenHashMap<>();
	@Unique
	private static long zakoopt$tick = Long.MIN_VALUE;
	@Unique
	private static WeakReference<ClientLevel> zakoopt$level = new WeakReference<>(null);

	// thousands of bubbles share a few columns; each checked its block's fluid every tick
	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
	private FluidState zakoopt$cachedFluid(ClientLevel level, BlockPos pos, Operation<FluidState> original) {
		if (!ZakoOptConfig.microOpts()) {
			return original.call(level, pos);
		}
		long tick = level.getGameTime();
		if (tick != zakoopt$tick || level != zakoopt$level.get()) {
			zakoopt$fluids.clear();
			zakoopt$tick = tick;
			zakoopt$level = new WeakReference<>(level);
		}
		long key = pos.asLong();
		FluidState fluid = zakoopt$fluids.get(key);
		if (fluid == null) {
			fluid = original.call(level, pos);
			zakoopt$fluids.put(key, fluid);
		}
		return fluid;
	}
}
