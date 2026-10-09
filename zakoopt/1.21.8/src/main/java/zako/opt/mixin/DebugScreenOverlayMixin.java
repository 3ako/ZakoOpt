package zako.opt.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.DebugInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(DebugScreenOverlay.class)
public class DebugScreenOverlayMixin {
	@ModifyReturnValue(method = "getGameInformation", at = @At("RETURN"))
	private List<String> zakoopt$status(List<String> lines) {
		List<String> out = new ArrayList<>(lines);
		out.add(DebugInfo.line());
		return out;
	}
}
