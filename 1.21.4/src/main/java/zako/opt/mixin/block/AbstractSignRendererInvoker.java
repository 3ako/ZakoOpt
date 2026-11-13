package zako.opt.mixin.block;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractSignRenderer.class)
public interface AbstractSignRendererInvoker {
	@Invoker("getSignModel")
	Model zakoopt$model(BlockState state, WoodType wood);

	@Invoker("getSignMaterial")
	Material zakoopt$material(WoodType wood);

	@Invoker("getSignModelRenderScale")
	float zakoopt$scale();

	@Invoker("translateSign")
	void zakoopt$translate(PoseStack poseStack, float yRot, BlockState state);
}
