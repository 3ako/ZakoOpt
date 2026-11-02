package zako.opt.platform;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import zako.opt.ZakoOptScreen;

@Mod(value = "zakoopt", dist = Dist.CLIENT)
public class NeoMod {
	public NeoMod(ModContainer container) {
		container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new ZakoOptScreen(parent));
	}
}
