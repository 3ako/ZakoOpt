package zako.opt.mixin.entity;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import zako.opt.entity.StateOwner;

@Mixin(EntityRenderState.class)
public class EntityRenderStateOwnerMixin implements StateOwner {
	@Unique
	private Object zakoopt$owner;

	@Override
	public Object zakoopt$owner() {
		return zakoopt$owner;
	}

	@Override
	public void zakoopt$owner(Object renderer) {
		zakoopt$owner = renderer;
	}
}
