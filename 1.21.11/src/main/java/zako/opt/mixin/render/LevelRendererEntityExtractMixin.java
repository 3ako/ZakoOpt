package zako.opt.mixin.render;

import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import zako.opt.entity.CullingFrustum;
import zako.opt.feature.Features;
import zako.opt.particle.Workers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

// the entity loop is two different jobs: cheap culling that reads shared level state, then building a fresh render state per
// entity, which only touches that entity. The culling stays in vanilla order on the render thread; the states are built in
// batches on the helpers and put back in the same order, so the submit pass sees exactly what it saw before
@Mixin(LevelRenderer.class)
public abstract class LevelRendererEntityExtractMixin {
	@Unique
	private static final int zakoopt$MIN_ENTITIES = 32;
	@Unique
	private static final int zakoopt$BATCH = 8;

	@Shadow
	private Minecraft minecraft;
	@Shadow
	private EntityRenderDispatcher entityRenderDispatcher;
	@Shadow
	private ClientLevel level;

	@Shadow
	protected abstract boolean shouldShowEntityOutlines();

	@Shadow
	public abstract boolean isSectionCompiledAndVisible(BlockPos blockPos);

	// reused between frames; the render thread is the only writer
	@Unique
	private final List<Entity> zakoopt$entities = new ArrayList<>();
	@Unique
	private float[] zakoopt$partials = new float[0];
	@Unique
	private EntityRenderState[] zakoopt$states = new EntityRenderState[0];

	@Inject(method = "extractVisibleEntities", at = @At("HEAD"), cancellable = true)
	private void zakoopt$parallelExtract(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState levelRenderState, CallbackInfo ci) {
		if (!Features.PARALLEL_ENTITIES.on) {
			return;
		}
		// EntityCulling reads this frustum on its worker; the vanilla loop is not there to set it any more
		CullingFrustum.update(frustum);
		ci.cancel();
		ClientLevel level = this.level;
		if (level == null) {
			return;
		}

		Vec3 pos = camera.position();
		double x = pos.x();
		double y = pos.y();
		double z = pos.z();
		TickRateManager ticks = level.tickRateManager();
		boolean outlines = this.shouldShowEntityOutlines();
		Entity.setViewScale(Mth.clamp(this.minecraft.options.getEffectiveRenderDistance() / 8.0, 1.0, 2.5) * this.minecraft.options.entityDistanceScaling().get());

		List<Entity> entities = this.zakoopt$entities;
		entities.clear();
		for (Entity entity : level.entitiesForRendering()) {
			if (!this.entityRenderDispatcher.shouldRender(entity, frustum, x, y, z) && !entity.hasIndirectPassenger(this.minecraft.player)) {
				continue;
			}
			BlockPos blockPos = entity.blockPosition();
			if (!level.isOutsideBuildHeight(blockPos.getY()) && !this.isSectionCompiledAndVisible(blockPos)) {
				continue;
			}
			if (entity == camera.entity() && !camera.isDetached()
				&& (!(camera.entity() instanceof LivingEntity living) || !living.isSleeping())) {
				continue;
			}
			if (entity instanceof LocalPlayer && camera.entity() != entity) {
				continue;
			}
			if (entity.tickCount == 0) {
				entity.xOld = entity.getX();
				entity.yOld = entity.getY();
				entity.zOld = entity.getZ();
			}
			entities.add(entity);
		}

		int n = entities.size();
		if (n == 0) {
			return;
		}
		if (this.zakoopt$partials.length < n) {
			this.zakoopt$partials = new float[n];
			this.zakoopt$states = new EntityRenderState[n];
		}
		float[] partials = this.zakoopt$partials;
		EntityRenderState[] states = this.zakoopt$states;
		for (int i = 0; i < n; i++) {
			partials[i] = deltaTracker.getGameTimeDeltaPartialTick(!ticks.isEntityFrozen(entities.get(i)));
		}

		if (n >= zakoopt$MIN_ENTITIES) {
			int blocks = (n + zakoopt$BATCH - 1) / zakoopt$BATCH;
			Throwable error = Workers.run(blocks, b -> {
				int from = b * zakoopt$BATCH;
				int to = Math.min(n, from + zakoopt$BATCH);
				for (int i = from; i < to; i++) {
					states[i] = ((LevelRendererExtractInvoker) this).zakoopt$extractEntity(entities.get(i), partials[i]);
				}
			});
			if (error != null) {
				if (error instanceof ReportedException reported) {
					throw reported;
				}
				throw new ReportedException(CrashReport.forThrowable(error, "Extracting entity render states (zakoopt parallel)"));
			}
		} else {
			for (int i = 0; i < n; i++) {
				states[i] = ((LevelRendererExtractInvoker) this).zakoopt$extractEntity(entities.get(i), partials[i]);
			}
		}

		for (int i = 0; i < n; i++) {
			EntityRenderState state = states[i];
			levelRenderState.entityRenderStates.add(state);
			if (outlines && state.appearsGlowing()) {
				levelRenderState.haveGlowingEntities = true;
			}
		}
	}
}
