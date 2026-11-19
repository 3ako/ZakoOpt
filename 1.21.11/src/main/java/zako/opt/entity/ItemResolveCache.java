package zako.opt.entity;

import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// the resolve stamp living on a render state: while it matches the next request the model build is skipped. It rides on
// the state itself, so a pooled state reused for another entity just misses and rebuilds. Only models whose output
// depends on nothing but the stack (the flat kind) may report kind 1; anything time-driven (crossbow pull, cooldown,
// compass) reports 2 and resolves every frame as before
public interface ItemResolveCache {
	boolean matches(ItemStack stack, ItemDisplayContext context, Level level, ItemOwner owner, int id, int entityTick, boolean usingItem);

	void stamp(ItemStack stack, ItemDisplayContext context, Level level, ItemOwner owner, int id, int entityTick, boolean usingItem, int kind);
}
