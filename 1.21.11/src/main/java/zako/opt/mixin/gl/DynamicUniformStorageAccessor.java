package zako.opt.mixin.gl;

import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.client.renderer.MappableRingBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DynamicUniformStorage.class)
public interface DynamicUniformStorageAccessor {
	@Accessor("lastUniform")
	DynamicUniformStorage.DynamicUniform zakoopt$lastUniform();

	@Accessor("lastUniform")
	void zakoopt$setLastUniform(DynamicUniformStorage.DynamicUniform uniform);

	@Accessor("ringBuffer")
	MappableRingBuffer zakoopt$ringBuffer();

	@Accessor("nextBlock")
	int zakoopt$nextBlock();

	@Accessor("nextBlock")
	void zakoopt$setNextBlock(int next);

	@Accessor("capacity")
	int zakoopt$capacity();

	@Accessor("blockSize")
	int zakoopt$blockSize();
}
