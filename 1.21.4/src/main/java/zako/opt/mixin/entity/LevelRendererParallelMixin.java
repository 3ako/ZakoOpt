package zako.opt.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import zako.opt.entity.ParallelModels;

import java.util.List;

@Mixin(LevelRenderer.class)
public class LevelRendererParallelMixin {
	@WrapMethod(method = "renderEntities")
	private void zakoopt$parallelModels(PoseStack poseStack, MultiBufferSource.BufferSource buffers, Camera camera, DeltaTracker delta, List<Entity> entities,
										Operation<Void> original) {
		ParallelModels.begin();
		try {
			original.call(poseStack, buffers, camera, delta, entities);
		} finally {
			ParallelModels.end();
		}
	}
}
