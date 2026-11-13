package zako.opt.mixin.block;

import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractSignRenderer.class)
public interface AbstractSignRendererInvoker {
	@Invoker("getSignModel")
	Model.Simple zakoopt$model(SignRenderState state);

	@Invoker("getSignSprite")
	SpriteId zakoopt$sprite(WoodType wood);
}
