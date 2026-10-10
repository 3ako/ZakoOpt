package zako.opt.entity;

import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import zako.opt.mixin.entity.ModelFeatureRendererInvoker;
import zako.opt.mixin.entity.ModelStorageAccessor;

// 26.1 renders solid and translucent models in two separate calls; each is its own parallel batch
@UtilityClass
public class ParallelModelHook {
	private final List<ParallelModels.Job> JOBS = new ArrayList<>();

	// false: too few models, vanilla renders them
	public boolean renderSolid(ModelFeatureRenderer renderer, ModelFeatureRenderer.Storage storage, MultiBufferSource.BufferSource buffers,
							   OutlineBufferSource outline, MultiBufferSource.BufferSource crumbling) {
		if (!ParallelModels.enabled()) {
			return false;
		}
		Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> solid = ((ModelStorageAccessor) storage).zakoopt$opaque();
		int submits = 0;
		for (List<SubmitNodeStorage.ModelSubmit<?>> list : solid.values()) {
			submits += list.size();
		}
		if (submits < ParallelModels.MIN_SUBMITS) {
			return false;
		}
		JOBS.clear();
		ParallelModels.begin();
		try {
			for (Map.Entry<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> e : solid.entrySet()) {
				for (SubmitNodeStorage.ModelSubmit<?> s : e.getValue()) {
					JOBS.add(add(s, e.getKey()));
				}
			}
		} finally {
			ParallelModels.end();
		}
		ModelFeatureRendererInvoker invoker = (ModelFeatureRendererInvoker) renderer;
		int k = 0;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			for (Map.Entry<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> e : solid.entrySet()) {
				VertexConsumer consumer = buffers.getBuffer(e.getKey());
				for (SubmitNodeStorage.ModelSubmit<?> s : e.getValue()) {
					emit(JOBS.get(k++), s, e.getKey(), consumer, stack, invoker, outline, crumbling);
				}
			}
		}
		return true;
	}

	public boolean renderTranslucent(ModelFeatureRenderer renderer, ModelFeatureRenderer.Storage storage, MultiBufferSource.BufferSource buffers,
									 OutlineBufferSource outline, MultiBufferSource.BufferSource crumbling) {
		if (!ParallelModels.enabled()) {
			return false;
		}
		List<SubmitNodeStorage.TranslucentModelSubmit<?>> translucent = ((ModelStorageAccessor) storage).zakoopt$translucent();
		if (translucent.size() < ParallelModels.MIN_SUBMITS) {
			return false;
		}
		translucent.sort(Comparator.comparingDouble(t -> -t.position().lengthSquared()));
		JOBS.clear();
		ParallelModels.begin();
		try {
			for (SubmitNodeStorage.TranslucentModelSubmit<?> t : translucent) {
				JOBS.add(add(t.modelSubmit(), t.renderType()));
			}
		} finally {
			ParallelModels.end();
		}
		ModelFeatureRendererInvoker invoker = (ModelFeatureRendererInvoker) renderer;
		int k = 0;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			for (SubmitNodeStorage.TranslucentModelSubmit<?> t : translucent) {
				emit(JOBS.get(k++), t.modelSubmit(), t.renderType(), buffers.getBuffer(t.renderType()), stack, invoker, outline, crumbling);
			}
		}
		return true;
	}

	// outlines and block cracks render the model a second time into other buffers: those few go the vanilla way
	private ParallelModels.Job add(SubmitNodeStorage.ModelSubmit<?> s, RenderType type) {
		if (s.outlineColor() != 0 || s.crumblingOverlay() != null || type.format() != ParallelModels.FORMAT) {
			return null;
		}
		return ParallelModels.add(s.model(), s.state(), s.pose(), s.lightCoords(), s.overlayCoords(), s.tintedColor(), s.sprite());
	}

	private void emit(ParallelModels.Job job, SubmitNodeStorage.ModelSubmit<?> s, RenderType type, VertexConsumer consumer, MemoryStack stack,
					  ModelFeatureRendererInvoker invoker, OutlineBufferSource outline, MultiBufferSource.BufferSource crumbling) {
		if (job == null || !ParallelModels.emit(job, consumer, stack)) {
			invoker.zakoopt$renderModel(s, type, consumer, outline, crumbling);
		}
	}
}
