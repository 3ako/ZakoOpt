package zako.opt.entity;

import lombok.experimental.UtilityClass;

// an entity drawn in a GUI (inventory preview) has GUI coordinates in its pose: distance checks would read it as far away
@UtilityClass
public class GuiEntities {
	public int depth;
}
