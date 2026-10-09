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
import zako.opt.ZakoOptConfig;

import java.util.Optional;
import java.util.OptionalDouble;

// HUD drawn into its own texture at the monitor refresh rate; frames in between only blend that texture in.
// Crosshair and camera overlays (vignette, pumpkin...) blend with the world, so they stay live every frame.
@UtilityClass
public class HudCache {
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

	private TextureTarget target;
	private boolean active;
	private boolean refresh;
	private boolean drawing;
	private long lastRefresh;
	public boolean suppressLiveParts;

	// once per frame, before any GUI extraction
	public void beginFrame(boolean renderLevel) {
		Minecraft mc = Minecraft.getInstance();
		boolean was = active;
		long now = System.nanoTime();
		active = HudWorth.update(now, MonitorRate.intervalNs(now)) && ZakoOptConfig.hudCache() && renderLevel && mc.level != null && mc.gui.screen() == null && mc.gui.overlay() == null;
		if (!active) {
			return;
		}
		int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
		refresh = !was || target == null || target.width != w || target.height != h || now - lastRefresh >= MonitorRate.intervalNs(now);
		if (refresh) {
			lastRefresh = now;
			if (target == null) {
				target = new TextureTarget("zakoopt hud", w, h, true, GpuFormat.RGBA8_UNORM);
			} else if (target.width != w || target.height != h) {
				target.resize(w, h);
			}
		}
	}

	public boolean active() {
		return active;
	}

	public boolean skipExtraction() {
		return active && !refresh;
	}

	public RenderTarget drawTarget() {
		return drawing ? target : null;
	}

	public RenderTarget redirect(RenderTarget requested) {
		return drawing && requested != target && requested == Minecraft.getInstance().gameRenderer.mainRenderTarget() ? target : null;
	}

	// depth is cleared to 0: 26.x renders with reversed depth
	public void drawIntoTexture(Runnable draw) {
		RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(), CLEAR, target.getDepthTexture(), 0.0);
		drawing = true;
		try {
			draw.run();
		} finally {
			drawing = false;
		}
	}

	public void composite() {
		RenderTarget main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
				.createRenderPass(() -> "zakoopt hud composite", main.getColorTextureView(), Optional.empty(),
						main.useDepth ? main.getDepthTextureView() : null, OptionalDouble.empty())) {
			pass.setPipeline(COMPOSITE);
			RenderSystem.bindDefaultUniforms(pass);
			pass.bindTexture("InSampler", target.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
			pass.draw(3, 1, 0, 0);
		}
	}
}
