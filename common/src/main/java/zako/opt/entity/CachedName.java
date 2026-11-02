package zako.opt.entity;

import net.minecraft.network.chat.Component;

public interface CachedName {
	Component zakoopt$name();

	long zakoopt$nameTick();

	void zakoopt$setName(Component name, long tick);
}
