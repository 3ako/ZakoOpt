package zako.opt.hud;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import zako.opt.ZakoOptConfig;

// HUD drawn into its own texture at the monitor refresh rate; frames in between only blend that texture in.
// Crosshair and camera overlays (vignette, pumpkin...) blend with the world, so they stay live every frame.
@UtilityClass
public class HudCache {
	// the HUD texture holds premultiplied colour: GUI blending onto transparent black leaves colour * alpha
	private final RenderPipeline COMPOSITE = RenderPipeline.builder()
			.withLocation(Identifier.fromNamespaceAndPath("zakoopt", "pipeline/hud_composite"))
			.withVertexShader("core/screenquad")
			.withFragmentShader("core/blit_screen")
			.withSampler("InSampler")
			.withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA), ColorTargetState.WRITE_COLOR))
			.withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
			.build();

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
		active = HudWorth.update(now, MonitorRate.intervalNs(now)) && ZakoOptConfig.hudCache() && renderLevel && mc.level != null && mc.screen == null && mc.getOverlay() == null;
		if (!active) {
			return;
		}
		int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
		refresh = !was || target == null || target.width != w || target.height != h || now - lastRefresh >= MonitorRate.intervalNs(now);
		if (refresh) {
			lastRefresh = now;
			if (target == null) {
				target = new TextureTarget("zakoopt hud", w, h, true);
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

	// picture-in-picture renderers (Xaero's minimap) render into their own texture and may read the real frame behind them
	public boolean pipPrepare;

	public RenderTarget redirect(RenderTarget requested) {
		return drawing && !pipPrepare && requested != target && requested == Minecraft.getInstance().getMainRenderTarget() ? target : null;
	}

	public void drawIntoTexture(Runnable draw) {
		RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(), 0, target.getDepthTexture(), 1.0);
		drawing = true;
		try {
			draw.run();
		} finally {
			drawing = false;
		}
	}

	public void composite() {
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
				.createRenderPass(() -> "zakoopt hud composite", main.getColorTextureView(), OptionalInt.empty(),
						main.useDepth ? main.getDepthTextureView() : null, OptionalDouble.empty())) {
			pass.setPipeline(COMPOSITE);
			RenderSystem.bindDefaultUniforms(pass);
			pass.bindTexture("InSampler", target.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
			pass.draw(0, 3);
		}
	}
}
