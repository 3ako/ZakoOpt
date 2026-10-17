package remodik.opt.mixin.entity;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import remodik.opt.entity.CachedName;
import remodik.opt.entity.CachedRenderer;
import remodik.opt.entity.PooledState;

@Getter
@Accessors(fluent = true, chain = false)
@Mixin(Entity.class)
public class EntityNameMixin implements CachedName, CachedRenderer, PooledState {
	@Unique
	private Component zakoopt$name;
	@Unique
	private long zakoopt$nameTick = Long.MIN_VALUE;
	@Unique
	private Object zakoopt$renderer;
	@Unique
	private Object zakoopt$rendererMap;
	@Unique
	private Object zakoopt$state;
	@Unique
	private Object zakoopt$stateOwner;

	@Override
	public void zakoopt$setName(Component name, long tick) {
		zakoopt$name = name;
		zakoopt$nameTick = tick;
	}

	@Override
	public void zakoopt$setRenderer(Object renderer, Object map) {
		zakoopt$renderer = renderer;
		zakoopt$rendererMap = map;
	}

	@Override
	public void zakoopt$setState(Object state, Object owner) {
		zakoopt$state = state;
		zakoopt$stateOwner = owner;
	}
}
