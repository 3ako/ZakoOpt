package zako.opt.mixin.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Gui.class)
public interface GuiInvoker {
	@Invoker("renderCameraOverlays")
	void zakoopt$renderCameraOverlays(GuiGraphics graphics, DeltaTracker deltaTracker);

	@Invoker("renderCrosshair")
	void zakoopt$renderCrosshair(GuiGraphics graphics, DeltaTracker deltaTracker);
}
