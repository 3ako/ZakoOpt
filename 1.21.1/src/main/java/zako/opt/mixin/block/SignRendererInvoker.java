package zako.opt.mixin.block;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SignRenderer.class)
public interface SignRendererInvoker {
	@Invoker("translateSign")
	void zakoopt$translate(PoseStack poseStack, float yRot, BlockState state);
}
