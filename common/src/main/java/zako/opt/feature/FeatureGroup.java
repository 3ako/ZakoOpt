package zako.opt.feature;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;

// a block of features under one heading, in this order
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum FeatureGroup {
	RENDER("render"),
	ITEMS("items"),
	SPAWNERS("spawners"),
	BLOCKS("blocks"),
	// shown only with Create installed: its features need Create's classes
	CREATE("create"),
	PARTICLES("particles"),
	INTERFACE("interface"),
	MEMORY("memory");

	private final String key;

	public Component title() {
		return Component.translatable("zakoopt.group." + key);
	}

	public Component description() {
		return Component.translatable("zakoopt.group." + key + ".description");
	}
}
