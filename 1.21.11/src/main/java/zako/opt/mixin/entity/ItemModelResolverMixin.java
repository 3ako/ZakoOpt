package zako.opt.mixin.entity;

import zako.opt.entity.ItemResolveCache;
import zako.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.EmptyModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.MissingItemModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;

// the model build for a hand/head item is skipped while nothing it reads has changed: the stack (by reference and by a
// content hash), its owner, the slot and the owner's tick. A composite or time-driven model (crossbow pull, cooldown,
// compass needle) resolves every frame as before, so no animation freezes
@Mixin(ItemModelResolver.class)
public class ItemModelResolverMixin {
	// the model the last resolve came through, read right after it ran; per thread, as entity extraction can run on workers
	@Unique
	private static final ThreadLocal<ItemModel> zakoopt$capturedModel = new ThreadLocal<>();

	// capture must not survive from one resolve into the next: a non-empty stack whose ITEM_MODEL component is absent
	// never reaches the model getter, and a stale model there would misjudge it
	@Unique
	private static void zakoopt$resetCapture() {
		zakoopt$capturedModel.remove();
	}

	@WrapMethod(method = "updateForTopItem")
	private void zakoopt$cachedResolve(
		ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, Level level, ItemOwner owner, int id, Operation<Void> original
	) {
		ItemResolveCache stamp = state instanceof ItemResolveCache cache ? cache : null;
		if (stamp == null || !Features.HELD_ITEM_CACHE.on) {
			original.call(state, stack, context, level, owner, id);
			return;
		}

		LivingEntity living = owner instanceof LivingEntity entity ? entity : null;
		int tick = living == null ? 0 : living.tickCount;
		boolean using = living != null && living.isUsingItem();
		if (stamp.matches(stack, context, level, owner, id, tick, using)) {
			return;
		}

		zakoopt$resetCapture();
		original.call(state, stack, context, level, owner, id);
		ItemModel captured = zakoopt$capturedModel.get();
		// the empty branch never reaches the model getter, and an absent capture must not mark it dynamic; its clear is
		// replayable all the same
		boolean flat = stack.isEmpty() || captured instanceof BlockModelWrapper
			|| captured instanceof EmptyModel || captured instanceof MissingItemModel;
		stamp.stamp(stack, context, level, owner, id, tick, using, flat ? 1 : 2);
	}

	// appendItemLayers calls Function.apply twice: clientProperties (gives ClientItem.Properties, not a model) and the
	// model getter. Only a result that really is a model is captured; the other one passes through untouched
	@WrapOperation(method = "appendItemLayers", at = @At(value = "INVOKE", target = "Ljava/util/function/Function;apply(Ljava/lang/Object;)Ljava/lang/Object;"))
	private Object zakoopt$capture(Function<Object, Object> getter, Object identifier, Operation<Object> original) {
		Object result = original.call(getter, identifier);
		if (result instanceof ItemModel model) {
			zakoopt$capturedModel.set(model);
		}
		return result;
	}
}
