package zako.opt.mixin.chunk;

import zako.opt.feature.Features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Deque;

// while the loading overlay is up, the frame budget does not matter: the whole light queue is applied at once instead of
// the 10% per frame vanilla takes below 1000 entries. In play the vanilla pacing stays exactly as it is
@Mixin(ClientLevel.class)
public abstract class ClientLevelLightMixin {
	@Shadow
	@Final
	private Deque<Runnable> lightUpdateQueue;

	@Inject(method = "pollLightUpdates", at = @At("HEAD"), cancellable = true)
	private void zakoopt$drainWhileLoading(CallbackInfo ci) {
		if (!Features.FAST_LIGHT.on || Minecraft.getInstance().getOverlay() == null) {
			return;
		}
		Runnable update;
		while ((update = this.lightUpdateQueue.poll()) != null) {
			update.run();
		}
		ci.cancel();
	}
}
