package zako.opt.mixin.particle;

import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.particle.AmbientBiomes;

@Mixin(ClientChunkCache.class)
public class ClientChunkCacheBiomesMixin {
	@Inject(method = "replaceBiomes", at = @At("TAIL"))
	private void zakoopt$biomesReplaced(int x, int z, FriendlyByteBuf buf, CallbackInfo ci) {
		AmbientBiomes.invalidate(x, z);
	}
}
