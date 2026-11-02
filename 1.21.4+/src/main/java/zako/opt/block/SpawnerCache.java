package zako.opt.block;

import zako.opt.feature.Features;

import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.level.BaseSpawner;

@UtilityClass
public class SpawnerCache {

	public boolean enabled() {
		return Features.SPAWNER_TICK_CACHE.on;
	}

	public EntityRenderState get(BaseSpawner spawner, long tick) {
		TickCached c = (TickCached) spawner;
		return c.zakoopt$cachedTick() == tick ? (EntityRenderState) c.zakoopt$cached() : null;
	}

	public void put(BaseSpawner spawner, long tick, EntityRenderState state) {
		((TickCached) spawner).zakoopt$setCached(state, tick);
	}
}
