package zako.opt.mixin.gl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.nio.ByteBuffer;
import zako.opt.gl.ImmediateRing;

// before 1.21.11 the draw lives in the composite subclass
@Mixin(targets = "net.minecraft.client.renderer.RenderType$CompositeRenderType")
public class RenderTypeDrawMixin {
	@Unique
	private static int zakoopt$baseVertex;

	@WrapOperation(method = "draw", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexFormat;uploadImmediateVertexBuffer(Ljava/nio/ByteBuffer;)Lcom/mojang/blaze3d/buffers/GpuBuffer;"))
	private GpuBuffer zakoopt$ringUpload(VertexFormat format, ByteBuffer data, Operation<GpuBuffer> original) {
		int base = ImmediateRing.upload(format, data);
		if (base < 0) {
			zakoopt$baseVertex = 0;
			return original.call(format, data);
		}
		zakoopt$baseVertex = base;
		return ImmediateRing.buffer(format);
	}

	@ModifyArg(method = "draw", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;drawIndexed(IIII)V"), index = 0)
	private int zakoopt$baseVertex(int baseVertex) {
		return baseVertex + zakoopt$baseVertex;
	}
}
