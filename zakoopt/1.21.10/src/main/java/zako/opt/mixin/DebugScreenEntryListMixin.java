package zako.opt.mixin;

import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(DebugScreenEntryList.class)
public abstract class DebugScreenEntryListMixin {
	@Shadow
	private Map<ResourceLocation, DebugScreenEntryStatus> allStatuses;

	@Shadow
	public abstract void rebuildCurrentList();

	// new entries are hidden unless a profile names them; ours shows in F3 until the player turns it off (F3+F6)
	@Inject(method = {"load", "loadProfile"}, at = @At("RETURN"))
	private void zakoopt$showByDefault(CallbackInfo ci) {
		if (allStatuses.putIfAbsent(ResourceLocation.fromNamespaceAndPath("zakoopt", "status"), DebugScreenEntryStatus.IN_F3) == null) {
			rebuildCurrentList();
		}
	}
}
