package zako.opt.mixin.gl;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.RenderType;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.gl.ImmediateRing;
import zako.opt.gl.RingBuilders;

@Mixin(ByteBufferBuilder.class)
public class ByteBufferBuilderMixin implements RingBuilders.Backed {
	@Shadow
	long pointer;
	@Shadow
	private long capacity;
	@Shadow
	private long writeOffset;
	@Shadow
	private int generation;
	@Unique
	private RenderType zakoopt$type;
	// 0 = vanilla memory, 1 = inside the immediate ring, 2 = ours but moved to malloc memory
	@Unique
	private int zakoopt$state;

	@Override
	public void zakoopt$attach(RenderType type, long address, int size) {
		MemoryUtil.getAllocator(false).free(pointer);
		pointer = address;
		capacity = size;
		zakoopt$type = type;
		zakoopt$state = 1;
	}

	@Override
	public boolean zakoopt$ringBacked() {
		return zakoopt$state != 0;
	}

	@Inject(method = "resize", at = @At("HEAD"), cancellable = true)
	private void zakoopt$grow(long size, CallbackInfo ci) {
		if (zakoopt$state != 1) {
			return;
		}
		long address = ImmediateRing.reserveBuilder(zakoopt$type.format(), (int) size);
		// the old region stays reserved until the segment comes round again; only the live count changes
		ImmediateRing.builderReleased();
		if (address == 0) {
			address = MemoryUtil.nmemAlloc(size);
			zakoopt$state = 2;
		}
		MemoryUtil.memCopy(pointer, address, writeOffset);
		pointer = address;
		capacity = size;
		ci.cancel();
	}

	@Inject(method = "close", at = @At("HEAD"), cancellable = true)
	private void zakoopt$close(CallbackInfo ci) {
		if (zakoopt$state == 0 || pointer == 0) {
			return;
		}
		RingBuilders.remember(zakoopt$type, capacity);
		if (zakoopt$state == 1) {
			ImmediateRing.builderReleased();
			pointer = 0;
			generation = -1;
			ci.cancel();
		} else {
			MemoryUtil.nmemFree(pointer);
			pointer = 0;
			generation = -1;
			ci.cancel();
		}
	}
}
