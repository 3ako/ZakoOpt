package zako.opt.mixin.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import zako.opt.entity.StateEntity;

@Getter
@Setter
@Accessors(fluent = true, chain = false)
@Mixin(EntityRenderState.class)
public class EntityRenderStateEntityMixin implements StateEntity {
	@Unique
	private Entity zakoopt$entity;
}
