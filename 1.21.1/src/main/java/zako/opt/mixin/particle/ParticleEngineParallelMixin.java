package zako.opt.mixin.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.particle.ParallelQuadParticles;

import java.util.Collections;
import java.util.Iterator;
import java.util.Queue;

// 1.21.1 renders every particle type inside render(): the type's queue is written in parallel at the loop's iterator() call
// the queue is that call's receiver: a by-type @Local breaks once another mod adds a Queue local to render()
@Mixin(ParticleEngine.class)
public class ParticleEngineParallelMixin {
	@Unique
	private BufferBuilder zakoopt$builder;
	@Unique
	private Camera zakoopt$camera;
	@Unique
	private float zakoopt$partialTick;

	@WrapOperation(method = {"render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V", "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"}, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/particle/ParticleRenderType;begin(Lcom/mojang/blaze3d/vertex/Tesselator;Lnet/minecraft/client/renderer/texture/TextureManager;)Lcom/mojang/blaze3d/vertex/BufferBuilder;"))
	private BufferBuilder zakoopt$begin(ParticleRenderType type, Tesselator tesselator, TextureManager textures, Operation<BufferBuilder> original,
										LightTexture light, Camera camera, float partialTick) {
		BufferBuilder builder = original.call(type, tesselator, textures);
		// only the vanilla sheets: a mod's own type may write another vertex format
		zakoopt$builder = type == ParticleRenderType.PARTICLE_SHEET_OPAQUE || type == ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
				|| type == ParticleRenderType.PARTICLE_SHEET_LIT ? builder : null;
		zakoopt$camera = camera;
		zakoopt$partialTick = partialTick;
		return builder;
	}

	@WrapOperation(method = {"render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V", "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"}, at = @At(value = "INVOKE", target = "Ljava/util/Queue;iterator()Ljava/util/Iterator;"))
	private Iterator<Particle> zakoopt$parallel(Queue<Particle> particles, Operation<Iterator<Particle>> original) {
		BufferBuilder builder = zakoopt$builder;
		zakoopt$builder = null;
		if (builder != null && ParallelQuadParticles.render(particles, builder, DefaultVertexFormat.PARTICLE, zakoopt$camera, zakoopt$partialTick)) {
			return Collections.emptyIterator();
		}
		return original.call(particles);
	}
}
