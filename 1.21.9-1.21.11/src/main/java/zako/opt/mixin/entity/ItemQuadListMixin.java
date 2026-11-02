package zako.opt.mixin.entity;

import zako.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;
import java.util.List;

// ArrayList.addAll copies its argument with toArray first: an Object[] per item layer per frame. The layer's list is cleared
// every frame and keeps its capacity, so adding the model's quads one by one makes nothing
@Mixin(BlockModelWrapper.class)
public class ItemQuadListMixin {
	@WrapOperation(method = "update", at = @At(value = "INVOKE", target = "Ljava/util/List;addAll(Ljava/util/Collection;)Z"))
	private boolean zakoopt$addQuads(List<Object> list, Collection<?> quads, Operation<Boolean> original) {
		if (!Features.FEWER_ALLOCATIONS.on || !(quads instanceof List<?> source)) {
			return original.call(list, quads);
		}
		for (int i = 0, n = source.size(); i < n; i++) {
			list.add(source.get(i));
		}
		return !source.isEmpty();
	}
}
