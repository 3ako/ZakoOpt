package zako.opt.mixin.entity;

import zako.opt.entity.ItemResolveCache;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

// one stamp per render state; a pooled state reused for another entity simply misses, because the stamp rides on the
// state and every part of the key changes with it
@Mixin(ItemStackRenderState.class)
@Implements(@Interface(iface = ItemResolveCache.class, prefix = "zakoopt$"))
public abstract class ItemStackResolveStampMixin {
	@Unique
	private ItemStack zakoopt$stampStack;
	@Unique
	private ItemDisplayContext zakoopt$stampContext;
	@Unique
	private Level zakoopt$stampLevel;
	@Unique
	private ItemOwner zakoopt$stampOwner;
	@Unique
	private int zakoopt$stampId;
	@Unique
	private int zakoopt$stampCount;
	@Unique
	private int zakoopt$stampHash;
	@Unique
	private int zakoopt$stampTick;
	@Unique
	private boolean zakoopt$stampUsing;
	@Unique
	private int zakoopt$stampKind;

	// kind 1 = the resolve produced nothing time-driven, so it is safe to replay as long as the key holds
	public boolean zakoopt$matches(ItemStack stack, ItemDisplayContext context, Level level, ItemOwner owner, int id, int entityTick, boolean usingItem) {
		return this.zakoopt$stampKind == 1 && stack == this.zakoopt$stampStack && stack.getCount() == this.zakoopt$stampCount
			&& ItemStack.hashItemAndComponents(stack) == this.zakoopt$stampHash
			&& context == this.zakoopt$stampContext && level == this.zakoopt$stampLevel && owner == this.zakoopt$stampOwner
			&& id == this.zakoopt$stampId && entityTick == this.zakoopt$stampTick && usingItem == this.zakoopt$stampUsing;
	}

	public void zakoopt$stamp(ItemStack stack, ItemDisplayContext context, Level level, ItemOwner owner, int id, int entityTick, boolean usingItem, int kind) {
		this.zakoopt$stampStack = stack;
		this.zakoopt$stampContext = context;
		this.zakoopt$stampLevel = level;
		this.zakoopt$stampOwner = owner;
		this.zakoopt$stampId = id;
		this.zakoopt$stampCount = stack.getCount();
		this.zakoopt$stampHash = ItemStack.hashItemAndComponents(stack);
		this.zakoopt$stampTick = entityTick;
		this.zakoopt$stampUsing = usingItem;
		this.zakoopt$stampKind = kind;
	}
}
