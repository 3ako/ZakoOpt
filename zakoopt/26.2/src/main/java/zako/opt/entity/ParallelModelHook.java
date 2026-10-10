package zako.opt.entity;

import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.List;
import zako.opt.mixin.entity.ModelFeatureRendererInvoker;
import zako.opt.mixin.entity.RenderTypeFeatureRendererInvoker;

// 26.2 hands models over in groups that are already ordered and batched; a group is one parallel batch
@UtilityClass
public class ParallelModelHook {
	private final List<ParallelModels.Job> JOBS = new ArrayList<>();

	// false: too few models in this group, vanilla builds it
	public boolean buildGroup(ModelFeatureRenderer renderer, List<ModelFeatureRenderer.Submit<?>> submits) {
		if (!ParallelModels.enabled() || submits.size() < ParallelModels.MIN_SUBMITS) {
			return false;
		}
		JOBS.clear();
		ParallelModels.begin();
		try {
			for (ModelFeatureRenderer.Submit<?> s : submits) {
				// block cracks go through a decal generator: those few go the vanilla way
				JOBS.add(s.sheetedDecalPose() != null || s.renderType().format() != ParallelModels.FORMAT ? null
						: ParallelModels.add(s.model(), s.state(), s.pose(), s.lightCoords(), s.overlayCoords(), s.tintedColor(), s.sprite()));
			}
		} finally {
			ParallelModels.end();
		}
		RenderTypeFeatureRendererInvoker builders = (RenderTypeFeatureRendererInvoker) renderer;
		ModelFeatureRendererInvoker vanilla = (ModelFeatureRendererInvoker) renderer;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			for (int k = 0; k < submits.size(); k++) {
				ModelFeatureRenderer.Submit<?> s = submits.get(k);
				ParallelModels.Job job = JOBS.get(k);
				// one builder call per model, as vanilla does: it decides where a new draw starts
				if (job == null || !ParallelModels.emit(job, builders.zakoopt$vertexBuilder(s.renderType()), stack)) {
					vanilla.zakoopt$prepareModel(s);
				}
			}
		}
		return true;
	}
}
