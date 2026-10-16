package zako.opt.mixin.gl;

import zako.opt.feature.Features;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.CompiledShaderProgram;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

// every draw bound its twelve samplers by "Sampler" + i, a new string each; here they are bound by fixed names
// and the vanilla loop is told it has none left to do
@Mixin(CompiledShaderProgram.class)
public abstract class ShaderSamplerNamesMixin {
	@Unique
	private static final String[] ZAKOOPT$NAMES = new String[12];

	static {
		for (int i = 0; i < ZAKOOPT$NAMES.length; i++) {
			ZAKOOPT$NAMES[i] = "Sampler" + i;
		}
	}

	@Shadow
	public abstract void bindSampler(String name, int texture);

	@ModifyConstant(method = "setDefaultUniforms", constant = @Constant(intValue = 12))
	private int zakoopt$samplers(int count) {
		if (!Features.FEWER_ALLOCATIONS.on) {
			return count;
		}
		for (int i = 0; i < ZAKOOPT$NAMES.length; i++) {
			bindSampler(ZAKOOPT$NAMES[i], RenderSystem.getShaderTexture(i));
		}
		return 0;
	}
}
