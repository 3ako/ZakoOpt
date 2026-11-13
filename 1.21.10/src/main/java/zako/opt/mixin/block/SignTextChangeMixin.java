package zako.opt.mixin.block;

import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.text.SignTextMesh;

// a sign with a baked board is no block entity to the renderer, so new text is picked up here
@Mixin(SignBlockEntity.class)
public class SignTextChangeMixin {
	@Inject(method = "loadAdditional", at = @At("TAIL"))
	private void zakoopt$loaded(ValueInput input, CallbackInfo ci) {
		SignTextMesh.changed((SignBlockEntity) (Object) this);
	}

	@Inject(method = "setText", at = @At("TAIL"))
	private void zakoopt$set(CallbackInfoReturnable<Boolean> cir) {
		SignTextMesh.changed((SignBlockEntity) (Object) this);
	}
}
