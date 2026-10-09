package zako.opt.mixin.particle;

import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.particle.ParallelParticleVertices;

@Mixin(QuadParticleRenderState.class)
public class QuadParticleRenderStateMixin {
	@Inject(method = "prepare", at = @At("HEAD"), cancellable = true)
	private void zakoopt$parallelVertices(ParticleFeatureRenderer.ParticleBufferCache cache, CallbackInfoReturnable<QuadParticleRenderState.PreparedBuffers> cir) {
		QuadParticleRenderState.PreparedBuffers prepared = ParallelParticleVertices.prepare((QuadParticleRenderState) (Object) this, cache);
		if (prepared != null) {
			cir.setReturnValue(prepared);
		}
	}
}
