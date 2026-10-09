package zako.opt.mixin;

import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.DebugInfo;

@Mixin(DebugScreenEntries.class)
public class DebugScreenEntriesMixin {
	@Shadow
	private static Identifier register(Identifier id, DebugScreenEntry entry) {
		throw new AssertionError();
	}

	@Inject(method = "<clinit>", at = @At("RETURN"))
	private static void zakoopt$register(CallbackInfo ci) {
		register(Identifier.fromNamespaceAndPath("zakoopt", "status"), (displayer, level, clientChunk, serverChunk) -> displayer.addLine(DebugInfo.line()));
	}
}
