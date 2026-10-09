package zako.opt.block;

import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.level.BaseSpawner;
import zako.opt.ZakoOptConfig;

@UtilityClass
public class SpawnerCache {

	public boolean enabled() {
		return ZakoOptConfig.spawnerTickCache();
	}

	public EntityRenderState get(BaseSpawner spawner, long tick) {
		TickCached c = (TickCached) spawner;
		return c.zakoopt$cachedTick() == tick ? (EntityRenderState) c.zakoopt$cached() : null;
	}

	public void put(BaseSpawner spawner, long tick, EntityRenderState state) {
		((TickCached) spawner).zakoopt$setCached(state, tick);
	}
}
