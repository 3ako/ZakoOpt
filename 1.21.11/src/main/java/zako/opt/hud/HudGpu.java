package zako.opt.hud;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.OptionalDouble;
import java.util.OptionalInt;

// the GPU side of the HUD cache, which changes with nearly every version
@UtilityClass
public class HudGpu {
	// the HUD texture holds premultiplied colour: GUI blending onto transparent black leaves colour * alpha
	private final RenderPipeline COMPOSITE = RenderPipeline.builder()
			.withLocation(Identifier.fromNamespaceAndPath("zakoopt", "pipeline/hud_composite"))
			.withVertexShader("core/screenquad")
			.withFragmentShader("core/blit_screen")
			.withSampler("InSampler")
			.withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
			.withDepthWrite(false)
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withColorWrite(true, false)
			.withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
			.build();

	public boolean noScreen(Minecraft mc) {
		return mc.screen == null && mc.getOverlay() == null;
	}

	public RenderTarget mainTarget(Minecraft mc) {
		return mc.getMainRenderTarget();
	}

	public TextureTarget createTarget(int w, int h) {
		return new TextureTarget("zakoopt hud", w, h, true);
	}

	public void clear(TextureTarget target) {
		RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(), 0, target.getDepthTexture(), 1.0);
	}

	// the whole HUD texture blended over the main target
	public void composite(TextureTarget hud, RenderTarget main) {
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
				.createRenderPass(() -> "zakoopt hud composite", main.getColorTextureView(), OptionalInt.empty(),
						main.useDepth ? main.getDepthTextureView() : null, OptionalDouble.empty())) {
			pass.setPipeline(COMPOSITE);
			RenderSystem.bindDefaultUniforms(pass);
			pass.bindTexture("InSampler", hud.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
			pass.draw(0, 3);
		}
	}
}
