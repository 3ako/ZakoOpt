package zako.opt;

import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.network.chat.Component;

@UtilityClass
public class OptionsHeader {
	public void add(OptionsList list, Component title) {
		list.addHeader(title);
	}
}
