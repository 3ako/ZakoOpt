package zako.opt.mixin.gl;

import zako.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.raphimc.immediatelyfast.feature.core.BatchableBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

// drawDirect takes a render type's set of builders out of the map once it has drawn them, and the next getBuffer of that type
// makes a new set: a fresh hash set with its arrays per render type per batch, the largest single source of garbage per frame
@Mixin(value = BatchableBufferSource.class, remap = false)
public class BatchableBufferSourceSetPoolMixin {
	@Unique
	private final ArrayDeque<Set<?>> zakoopt$free = new ArrayDeque<>();
	@Unique
	private Set<?> zakoopt$drawn;

	@WrapOperation(method = "drawDirect", at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;"))
	private Object zakoopt$keepSet(Map<?, ?> map, Object key, Operation<Object> original) {
		zakoopt$recycle();
		Object set = original.call(map, key);
		if (Features.FEWER_ALLOCATIONS.on && set instanceof Set<?> s) {
			zakoopt$drawn = s;
		}
		return set;
	}

	@WrapOperation(method = {"getBuffer", "method_73477"}, at = @At(value = "INVOKE", target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"))
	private Object zakoopt$reuseSet(Map<Object, Object> map, Object key, Function<Object, Object> make, Operation<Object> original) {
		zakoopt$recycle();
		if (zakoopt$free.isEmpty()) {
			return original.call(map, key, make);
		}
		Object set = map.get(key);
		if (set == null) {
			set = zakoopt$free.pop();
			map.put(key, set);
		}
		return set;
	}

	// drawDirect walks the set it took out only within that call: by the next call it can be emptied and handed out again
	@Unique
	private void zakoopt$recycle() {
		if (zakoopt$drawn != null) {
			zakoopt$drawn.clear();
			zakoopt$free.push(zakoopt$drawn);
			zakoopt$drawn = null;
		}
	}
}
