package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;
import zako.opt.block.SpawnerMeshCache;

@Mixin(SpawnerRenderer.class)
public class SpawnerRendererMixin {
	// the spinning mob is a full entity render per spawner per frame; far away it is a few pixels
	@WrapOperation(method = "render(Lnet/minecraft/world/level/block/entity/SpawnerBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/SpawnerRenderer;renderEntityInSpawner(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;DD)V"))
	private void zakoopt$cull(float partialTick, PoseStack pose, MultiBufferSource buffers, int light, Entity entity, EntityRenderDispatcher dispatcher, double oSpin, double spin,
							  Operation<Void> original, SpawnerBlockEntity spawner, float partialTick2, PoseStack pose2, MultiBufferSource buffers2, int light2, int overlay) {
		if (ZakoOptConfig.spawnerCull()) {
			double d = ZakoOptConfig.spawnerDistance();
			if (Vec3.atCenterOf(spawner.getBlockPos()).distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()) > d * d) {
				return;
			}
		}
		original.call(partialTick, pose, buffers, light, entity, dispatcher, oSpin, spin);
	}

	// the mob never moves inside the spawner: drawn once, then replayed through the spinning pose
	@WrapOperation(method = "renderEntityInSpawner", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
	private static void zakoopt$cachedMob(EntityRenderDispatcher dispatcher, Entity entity, double x, double y, double z, float partialTick, PoseStack pose,
										  MultiBufferSource buffers, int light, Operation<Void> original) {
		if (!ZakoOptConfig.spawnerMesh() || !SpawnerMeshCache.render(entity, dispatcher, pose, buffers, light,
				(recorder, mark) -> original.call(dispatcher, entity, x, y, z, partialTick, new PoseStack(), recorder, mark))) {
			original.call(dispatcher, entity, x, y, z, partialTick, pose, buffers, light);
		}
	}
}
