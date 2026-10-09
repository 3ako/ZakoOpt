package zako.opt.mixin.entity;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import zako.opt.entity.CachedName;

@Getter
@Accessors(fluent = true, chain = false)
@Mixin(Entity.class)
public class EntityNameMixin implements CachedName {
	@Unique
	private Component zakoopt$name;
	@Unique
	private long zakoopt$nameTick = Long.MIN_VALUE;

	@Override
	public void zakoopt$setName(Component name, long tick) {
		zakoopt$name = name;
		zakoopt$nameTick = tick;
	}
}
