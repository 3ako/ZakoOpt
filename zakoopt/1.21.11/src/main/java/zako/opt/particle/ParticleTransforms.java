package zako.opt.particle;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.experimental.UtilityClass;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@UtilityClass
public class ParticleTransforms {
	// the dynamic transform uniforms the particle draws use: model-view, white, no offset
	public GpuBufferSlice write() {
		return RenderSystem.getDynamicUniforms()
				.writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
	}
}
