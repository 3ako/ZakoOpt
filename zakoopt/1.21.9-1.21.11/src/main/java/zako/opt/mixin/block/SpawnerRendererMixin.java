package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;
import zako.opt.block.SpawnerCache;
import zako.opt.block.SpawnerReplay;

@Mixin(SpawnerRenderer.class)
public class SpawnerRendererMixin {
	@WrapOperation(method = "extractRenderState(Lnet/minecraft/world/level/block/entity/SpawnerBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/SpawnerRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/TrialSpawnerRenderer;extractSpawnerData(Lnet/minecraft/client/renderer/blockentity/state/SpawnerRenderState;FLnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;DD)V"))
	private void zakoopt$skipFarDisplayEntity(SpawnerRenderState state, float partialTick, Entity entity, EntityRenderDispatcher dispatcher, double oSpin, double spin,
											  Operation<Void> original, SpawnerBlockEntity spawner, SpawnerRenderState state2, float f, Vec3 camera,
											  ModelFeatureRenderer.CrumblingOverlay crumbling) {
		double distSq = Vec3.atCenterOf(spawner.getBlockPos()).distanceToSqr(camera);
		if (ZakoOptConfig.spawnerCull()) {
			double d = ZakoOptConfig.spawnerDistance();
			if (distSq > d * d) {
				state.displayEntity = null;
				return;
			}
		}
		if (entity == null || !SpawnerCache.enabled()) {
			original.call(state, partialTick, entity, dispatcher, oSpin, spin);
		} else {
			long tick = entity.level().getGameTime();
			EntityRenderState cached = SpawnerCache.get(spawner.getSpawner(), tick);
			if (cached == null) {
				original.call(state, partialTick, entity, dispatcher, oSpin, spin);
				SpawnerCache.put(spawner.getSpawner(), tick, state.displayEntity);
			} else {
				// same as TrialSpawnerRenderer.extractSpawnerData, minus the per-frame extractEntity
				state.displayEntity = cached;
				cached.lightCoords = state.lightCoords;
				state.spin = (float) Mth.lerp(partialTick, oSpin, spin) * 10.0F;
				state.scale = 0.53125F;
				float size = Math.max(entity.getBbWidth(), entity.getBbHeight());
				if (size > 1.0F) {
					state.scale /= size;
				}
			}
		}
		if (ZakoOptConfig.spawnerReplay() && distSq > SpawnerReplay.DISTANCE * SpawnerReplay.DISTANCE) {
			// far spawners turn once per tick so SpawnerReplay can reuse the whole tick's submits
			state.spin = (float) spin * 10.0F;
		}
	}

	@WrapOperation(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/SpawnerRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/SpawnerRenderer;submitEntityInSpawner(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;FFLnet/minecraft/client/renderer/state/CameraRenderState;)V"))
	private void zakoopt$replayFar(PoseStack pose, SubmitNodeCollector collector, EntityRenderState entity, EntityRenderDispatcher dispatcher, float spin, float scale,
								   CameraRenderState camera, Operation<Void> original, SpawnerRenderState state) {
		Minecraft mc = Minecraft.getInstance();
		if (!ZakoOptConfig.spawnerReplay() || !SpawnerCache.enabled() || mc.level == null
				|| Vec3.atCenterOf(state.blockPos).distanceToSqr(camera.pos) <= SpawnerReplay.DISTANCE * SpawnerReplay.DISTANCE) {
			original.call(pose, collector, entity, dispatcher, spin, scale, camera);
			return;
		}
		SpawnerReplay.submit(entity, mc.level.getGameTime(), camera.pos, collector,
				c -> original.call(pose, c, entity, dispatcher, spin, scale, camera));
	}
}
