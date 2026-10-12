package zako.opt.bench;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

// the view of the test scene a feature is measured on, in the order the camera visits them; key = the scene's view
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum BenchView {
	NONE(""),
	TERRAIN("terrain"),
	OVERVIEW("overview"),
	CROWD("crowd"),
	ITEMS("items"),
	BLOCKS("blocks"),
	PISTONS("pistons"),
	HOLOGRAMS("holograms");

	private final String key;
}
