package zako.opt.block;

import zako.opt.feature.Features;

import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

@UtilityClass
public class BlockEntityDistance {
	// distance check per block entity per frame; a tick late at the edge of view distance is invisible
	@SuppressWarnings({"unchecked", "rawtypes"})
	public boolean inDistance(BlockEntityRenderer renderer, BlockEntity blockEntity, Vec3 camera) {
		if (!Features.MICRO_OPTS.fewerChecks || blockEntity.getLevel() == null) {
			return renderer.shouldRender(blockEntity, camera);
		}
		CachedValidity cache = (CachedValidity) blockEntity;
		long tick = blockEntity.getLevel().getGameTime();
		if (cache.zakoopt$distanceTick() != tick) {
			cache.zakoopt$setInDistance(renderer.shouldRender(blockEntity, camera), tick);
		}
		return cache.zakoopt$inDistance();
	}
}
