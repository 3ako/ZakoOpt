package zako.opt.mixin.block;

import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChestRenderer.class)
public interface ChestRendererInvoker {
	@Invoker("getChestMaterial")
	static ChestRenderState.ChestMaterialType zakoopt$material(BlockEntity blockEntity, boolean xmas) {
		throw new AssertionError();
	}
}
