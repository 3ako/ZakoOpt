package zako.opt.hud;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import org.joml.Vector4f;

import java.util.Optional;
import java.util.OptionalDouble;

// the GPU side of the HUD cache, which changes with nearly every version
@UtilityClass
public class HudGpu {
	// the HUD texture holds premultiplied colour: GUI blending onto transparent black leaves colour * alpha; write mask 7 = RGB only
	private final RenderPipeline COMPOSITE = RenderPipeline.builder()
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			.withLocation(Identifier.fromNamespaceAndPath("zakoopt", "pipeline/hud_composite"))
			.withVertexShader("core/screenquad")
			.withFragmentShader("core/blit_screen")
			.withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
			.withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA), GpuFormat.RGBA8_UNORM, 7))
			.withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
			.build();
	private final Vector4f CLEAR = new Vector4f(0, 0, 0, 0);

	public boolean noScreen(Minecraft mc) {
		return mc.gui.screen() == null && mc.gui.overlay() == null;
	}

	public RenderTarget mainTarget(Minecraft mc) {
		return mc.gameRenderer.mainRenderTarget();
	}

	public TextureTarget createTarget(int w, int h) {
		return new TextureTarget("zakoopt hud", w, h, true, GpuFormat.RGBA8_UNORM);
	}

	// depth is cleared to 0: 26.x renders with reversed depth
	public void clear(TextureTarget target) {
		RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(), CLEAR, target.getDepthTexture(), 0.0);
	}

	// the whole HUD texture blended over the main target
	public void composite(TextureTarget hud, RenderTarget main) {
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
				.createRenderPass(() -> "zakoopt hud composite", main.getColorTextureView(), Optional.empty(),
						main.useDepth ? main.getDepthTextureView() : null, OptionalDouble.empty())) {
			pass.setPipeline(COMPOSITE);
			RenderSystem.bindDefaultUniforms(pass);
			pass.bindTexture("InSampler", hud.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
			pass.draw(3, 1, 0, 0);
		}
	}
}
