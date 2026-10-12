package zako.opt;

import lombok.experimental.UtilityClass;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.network.chat.Component;
import zako.opt.hud.HudWorth;

// why an option is held off on this setup whatever its setting; null: it works as set
@UtilityClass
public class Blocked {
	public Component reason(String key) {
		String cause = switch (key) {
			case "hud_cache" -> MixinPlugin.VULKAN ? "VulkanMod" : ZakoOptConfig.FOREIGN_HUD ? ZakoOptConfig.FOREIGN_HUD_MOD
					: HudWorth.isMainSwapped() ? "zakoopt.blocked.other_mod" : null;
			case "skin_atlas" -> MixinPlugin.VULKAN ? "VulkanMod" : null;
			case "parallel_models" -> ZakoOptConfig.CORES < 4 ? "zakoopt.blocked.cpu" : shaders() ? "Iris" : null;
			case "parallel_particles" -> ZakoOptConfig.CORES < 4 ? "zakoopt.blocked.cpu" : null;
			case "ring_zero_copy" -> shaders() ? "Iris" : null;
			case "core_pin" -> zako.opt.cpu.CorePin.available() ? null : "zakoopt.blocked.cores_same";
			default -> null;
		};
		return cause == null ? null : Component.translatable("zakoopt.blocked",
				cause.startsWith("zakoopt.") ? Component.translatable(cause) : Component.literal(cause));
	}

	private boolean shaders() {
		return ZakoOptConfig.IRIS && IrisApi.getInstance().isShaderPackInUse();
	}
}
