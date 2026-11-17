package zako.opt.mixin.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import zako.opt.entity.StateOwner;

@Getter
@Setter
@Accessors(fluent = true, chain = false)
@Mixin(EntityRenderState.class)
public class EntityRenderStateOwnerMixin implements StateOwner {
	@Unique
	private Object zakoopt$owner;
}
