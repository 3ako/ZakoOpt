package remodik.opt.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// every mob holding a crossbow asks for its charge time per frame to pose the arm, and the enchantment pass behind it made a
// MutableFloat, a lambda and an iterator each time. With no enchantments the pass changes nothing: the time comes back as is
@Mixin(CrossbowItem.class)
public class CrossbowChargeMixin {
	@WrapOperation(method = "getChargeDuration", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;modifyCrossbowChargingTime(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;F)F"))
	private static float zakoopt$chargeTime(ItemStack stack, LivingEntity holder, float time, Operation<Float> original) {
		return stack.getEnchantments().isEmpty() ? Math.max(0.0F, time) : original.call(stack, holder, time);
	}
}
