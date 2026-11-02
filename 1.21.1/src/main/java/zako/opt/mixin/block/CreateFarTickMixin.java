package zako.opt.mixin.block;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.feature.Features;

@Pseudo
@Mixin(value = SmartBlockEntityTicker.class, remap = false)
public class CreateFarTickMixin {
	// on the client a Create block entity ticks only its looks (belt items, flaps, chute items), which nobody sees past the
	// 64 blocks block entities are drawn to; the server keeps the real state and syncs it, so a skipped one is current again on approach.
	// Sable keeps sub-level blocks in plots far past the view distance, and those tick as before
	@Inject(require = 0, method = "tick", at = @At("HEAD"), cancellable = true)
	private void zakoopt$farTick(Level level, BlockPos pos, BlockState state, BlockEntity be, CallbackInfo ci) {
		if (!level.isClientSide || !Features.CREATE_FAR_TICK.on) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
		double dx = pos.getX() + 0.5 - cam.x, dz = pos.getZ() + 0.5 - cam.z, dy = pos.getY() + 0.5 - cam.y;
		double d = dx * dx + dy * dy + dz * dz;
		double view = mc.options.getEffectiveRenderDistance() * 16 + 32;
		int far = Features.CREATE_FAR_TICK.distance;
		if (d > far * far && dx * dx + dz * dz < view * view) {
			ci.cancel();
		}
	}
}
