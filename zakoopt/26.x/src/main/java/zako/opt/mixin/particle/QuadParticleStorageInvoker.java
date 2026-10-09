package zako.opt.mixin.particle;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.client.renderer.state.level.QuadParticleRenderState$Storage")
public interface QuadParticleStorageInvoker {
	@Invoker("grow")
	void zakoopt$grow();

	@Accessor("capacity")
	int zakoopt$capacity();

	@Accessor("floatValues")
	float[] zakoopt$floats();

	@Accessor("intValues")
	int[] zakoopt$ints();

	@Accessor("currentParticleIndex")
	int zakoopt$count();

	@Accessor("currentParticleIndex")
	void zakoopt$setCount(int count);
}
