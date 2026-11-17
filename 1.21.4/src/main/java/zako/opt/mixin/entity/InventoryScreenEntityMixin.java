package zako.opt.mixin.entity;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.entity.GuiEntities;

@Mixin(InventoryScreen.class)
public class InventoryScreenEntityMixin {
	@Inject(method = "renderEntityInInventory", at = @At("HEAD"))
	private static void zakoopt$guiStart(CallbackInfo ci) {
		GuiEntities.depth++;
	}

	@Inject(method = "renderEntityInInventory", at = @At("RETURN"))
	private static void zakoopt$guiEnd(CallbackInfo ci) {
		GuiEntities.depth--;
	}
}
