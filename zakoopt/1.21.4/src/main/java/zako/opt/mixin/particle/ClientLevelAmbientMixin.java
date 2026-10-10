package zako.opt.mixin.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.ZakoOptConfig;
import zako.opt.particle.AmbientBiomes;

@Mixin(ClientLevel.class)
public class ClientLevelAmbientMixin {
	@WrapOperation(method = "doAnimateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBiome(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;"))
	private Holder<Biome> zakoopt$ambientBiome(ClientLevel level, BlockPos pos, Operation<Holder<Biome>> original) {
		if (!ZakoOptConfig.microOpts3()) {
			return original.call(level, pos);
		}
		return AmbientBiomes.biome(level, pos, p -> original.call(level, p));
	}

	@Inject(method = "onChunkLoaded", at = @At("TAIL"))
	private void zakoopt$chunkLoaded(ChunkPos pos, CallbackInfo ci) {
		AmbientBiomes.invalidate(pos.x, pos.z);
	}
}
