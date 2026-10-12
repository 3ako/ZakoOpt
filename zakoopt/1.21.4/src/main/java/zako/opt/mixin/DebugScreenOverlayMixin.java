package zako.opt.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.DebugInfo;
import zako.opt.feature.Features;
import zako.opt.gl.DrawStats;
import zako.opt.gl.GpuTimers;
import zako.opt.gl.OcclusionCull;

import java.util.ArrayList;
import java.util.List;

@Mixin(DebugScreenOverlay.class)
public class DebugScreenOverlayMixin {
	@ModifyReturnValue(method = "getGameInformation", at = @At("RETURN"))
	private List<String> zakoopt$status(List<String> lines) {
		List<String> out = new ArrayList<>(lines);
		out.add(DebugInfo.line());
		out.addAll(DrawStats.lines());
		out.addAll(GpuTimers.lines());
		if (Features.OCCLUSION_CULL.on) {
			out.add(OcclusionCull.line());
		}
		return out;
	}
}
