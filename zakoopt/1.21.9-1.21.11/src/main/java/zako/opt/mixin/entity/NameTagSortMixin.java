package zako.opt.mixin.entity;

import zako.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Comparator;
import java.util.function.Function;

// the see-through tags are sorted by Comparator.comparing(distanceToCameraSq): a boxed Double on every comparison, n log n of
// them per frame (3% of the render thread's garbage with a crowd of named mobs); the same order on the primitive
@Mixin(NameTagFeatureRenderer.class)
public class NameTagSortMixin {
	@Unique
	private static final Comparator<SubmitNodeStorage.NameTagSubmit> zakoopt$byDistance = Comparator.comparingDouble(SubmitNodeStorage.NameTagSubmit::distanceToCameraSq);

	@SuppressWarnings({"rawtypes", "unchecked"})
	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/Comparator;comparing(Ljava/util/function/Function;)Ljava/util/Comparator;"))
	private Comparator zakoopt$primitiveSort(Function key, Operation<Comparator> original) {
		return Features.FEWER_ALLOCATIONS.on ? zakoopt$byDistance : original.call(key);
	}
}
