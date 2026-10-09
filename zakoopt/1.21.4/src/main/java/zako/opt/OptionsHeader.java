package zako.opt;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;

import java.util.List;

@UtilityClass
public class OptionsHeader {
	public void add(OptionsList list, Component title) {
		list.addSmall(List.<AbstractWidget>of(new StringWidget(310, 20, title, Minecraft.getInstance().font)));
	}
}
