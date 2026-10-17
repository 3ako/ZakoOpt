package remodik.opt;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// NeoForge entry point: the mod works through its mixins, this only puts the options screen into the mods list
@Mod(value = "zakoopt", dist = Dist.CLIENT)
public class ZakoOptMod {
	public ZakoOptMod(ModContainer container) {
		container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new ZakoOptScreen(parent));
	}
}
