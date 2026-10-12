package zako.opt.block;

import zako.opt.feature.Features;

import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.BooleanSupplier;

@UtilityClass
public class BlockEntityDistance {
	// distance check per block entity per frame; a tick late at the edge of view distance is invisible
	public boolean inDistance(BlockEntity blockEntity, BooleanSupplier check) {
		if (!Features.MICRO_OPTS.fewerChecks || blockEntity.getLevel() == null) {
			return check.getAsBoolean();
		}
		CachedValidity cache = (CachedValidity) blockEntity;
		long tick = blockEntity.getLevel().getGameTime();
		if (cache.zakoopt$distanceTick() != tick) {
			cache.zakoopt$setInDistance(check.getAsBoolean(), tick);
		}
		return cache.zakoopt$inDistance();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public boolean inDistance(BlockEntityRenderer renderer, BlockEntity blockEntity, Vec3 camera) {
		return inDistance(blockEntity, () -> renderer.shouldRender(blockEntity, camera));
	}
}
