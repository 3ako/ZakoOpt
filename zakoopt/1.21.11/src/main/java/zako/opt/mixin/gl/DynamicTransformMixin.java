package zako.opt.mixin.gl;

import zako.opt.feature.Features;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.client.renderer.DynamicUniforms;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

// every draw copied its four transform values into new matrices and vectors wrapped in a new Transform, written to the
// uniform buffer at once. The storage keeps the last one written to skip a repeat with equals, and on a repeat it keeps the
// older one: so of two reused Transforms the one refilled is always the one it is not keeping
@Mixin(DynamicUniforms.class)
public class DynamicTransformMixin {
	@Shadow
	@Final
	private DynamicUniformStorage<DynamicUniforms.Transform> transforms;
	@Unique
	private final Matrix4f[] zakoopt$modelView = {new Matrix4f(), new Matrix4f()};
	@Unique
	private final Vector4f[] zakoopt$color = {new Vector4f(), new Vector4f()};
	@Unique
	private final Vector3f[] zakoopt$offset = {new Vector3f(), new Vector3f()};
	@Unique
	private final Matrix4f[] zakoopt$texture = {new Matrix4f(), new Matrix4f()};
	@Unique
	private final DynamicUniforms.Transform[] zakoopt$transform = new DynamicUniforms.Transform[2];

	@Overwrite
	public GpuBufferSlice writeTransform(Matrix4fc modelView, Vector4fc color, Vector3fc offset, Matrix4fc texture) {
		if (!Features.FEWER_ALLOCATIONS.on || !RenderSystem.isOnRenderThread()) {
			return transforms.writeUniform(new DynamicUniforms.Transform(new Matrix4f(modelView), new Vector4f(color), new Vector3f(offset), new Matrix4f(texture)));
		}
		int i = zakoopt$transform[0] != null && zakoopt$transform[0] == ((DynamicUniformStorageAccessor) transforms).zakoopt$lastUniform() ? 1 : 0;
		if (zakoopt$transform[i] == null) {
			zakoopt$transform[i] = new DynamicUniforms.Transform(zakoopt$modelView[i], zakoopt$color[i], zakoopt$offset[i], zakoopt$texture[i]);
		}
		zakoopt$modelView[i].set(modelView);
		zakoopt$color[i].set(color);
		zakoopt$offset[i].set(offset);
		zakoopt$texture[i].set(texture);
		return transforms.writeUniform(zakoopt$transform[i]);
	}
}
