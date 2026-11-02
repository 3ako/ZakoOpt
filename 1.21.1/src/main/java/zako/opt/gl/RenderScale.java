package zako.opt.gl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import zako.opt.feature.Features;
import zako.opt.mixin.gl.LevelRendererScaleAccessor;
import zako.opt.mixin.gl.MinecraftTargetAccessor;

// the world is drawn into a smaller target that stands in for the main one, then one pass stretches it into the real main
// target and sharpens it with AMD FSR 1 RCAS. The GUI is drawn afterwards into the main target as usual, at full resolution
@Slf4j(topic = "zakoopt")
@UtilityClass
public class RenderScale {
	private TextureTarget low;
	private RenderTarget full;
	private int program, vao, uSource, uSize;
	private boolean ready, failed;

	public void begin(Minecraft mc) {
		full = null;
		// 1.21.1's Fabulous mode keeps its own window-sized targets beside the main one; they would not match a scaled main target
		if (!Features.RENDER_SCALE.on || Minecraft.useShaderTransparency() || failed || !ready && !init()) {
			resizeOutline(mc, mc.getWindow().getWidth(), mc.getWindow().getHeight());
			return;
		}
		RenderTarget main = mc.getMainRenderTarget();
		int w = Math.max(1, main.width * Features.RENDER_SCALE.percent / 100), h = Math.max(1, main.height * Features.RENDER_SCALE.percent / 100);
		if (low == null) {
			low = new TextureTarget(w, h, true, Minecraft.ON_OSX);
			low.setClearColor(0, 0, 0, 0);
		} else if (low.width != w || low.height != h) {
			low.resize(w, h, Minecraft.ON_OSX);
		}
		// resize() puts the filter back to nearest
		low.setFilterMode(GL11C.GL_LINEAR);
		// the outline post chain reads the main target's size, and its blit sets the viewport to its own
		resizeOutline(mc, w, h);
		full = main;
		((MinecraftTargetAccessor) mc).zakoopt$setMainRenderTarget(low);
		low.bindWrite(true);
	}

	public void end(Minecraft mc) {
		if (full == null) {
			return;
		}
		GpuTimers.mark("render scale");
		((MinecraftTargetAccessor) mc).zakoopt$setMainRenderTarget(full);
		upscale(full);
		full.bindWrite(true);
		full = null;
	}

	private void resizeOutline(Minecraft mc, int w, int h) {
		RenderTarget outline = ((LevelRendererScaleAccessor) mc.levelRenderer).zakoopt$entityOutlineTarget();
		if (outline != null && (outline.width != w || outline.height != h)) {
			outline.resize(w, h, Minecraft.ON_OSX);
		}
	}

	private void upscale(RenderTarget main) {
		int prevProgram = GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM), prevVao = GL11C.glGetInteger(GL30C.GL_VERTEX_ARRAY_BINDING);
		int prevUnit = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
		GL13C.glActiveTexture(GL13C.GL_TEXTURE0);
		int prevTexture = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
		boolean depthTest = GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST), blend = GL11C.glIsEnabled(GL11C.GL_BLEND), cull = GL11C.glIsEnabled(GL11C.GL_CULL_FACE),
				scissor = GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST);
		boolean depthMask = GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK);

		GL11C.glDisable(GL11C.GL_DEPTH_TEST);
		GL11C.glDisable(GL11C.GL_BLEND);
		GL11C.glDisable(GL11C.GL_CULL_FACE);
		GL11C.glDisable(GL11C.GL_SCISSOR_TEST);
		GL11C.glDepthMask(false);
		GL30C.glBindVertexArray(vao);
		GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, main.frameBufferId);
		GL11C.glViewport(0, 0, main.width, main.height);
		GL20C.glUseProgram(program);
		GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, low.getColorTextureId());
		GL20C.glUniform1i(uSource, 0);
		GL20C.glUniform2f(uSize, main.width, main.height);
		GL11C.glDrawArrays(GL11C.GL_TRIANGLES, 0, 3);

		GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, prevTexture);
		GL13C.glActiveTexture(prevUnit);
		GL20C.glUseProgram(prevProgram);
		GL30C.glBindVertexArray(prevVao);
		GL11C.glDepthMask(depthMask);
		toggle(GL11C.GL_DEPTH_TEST, depthTest);
		toggle(GL11C.GL_BLEND, blend);
		toggle(GL11C.GL_CULL_FACE, cull);
		toggle(GL11C.GL_SCISSOR_TEST, scissor);
	}

	private void toggle(int cap, boolean on) {
		if (on) {
			GL11C.glEnable(cap);
		} else {
			GL11C.glDisable(cap);
		}
	}

	private boolean init() {
		try {
			program = program("""
					#version 150 core
					// bilinear taps a source texel apart, sharpened with FSR 1 RCAS weights
					uniform sampler2D source;
					uniform vec2 size;
					out vec4 color;

					// exp2(-0.2): FSR's default strength of 0.2 stops
					const float SHARPNESS = 0.8706;

					void main() {
						vec2 uv = gl_FragCoord.xy / size, t = 1.0 / vec2(textureSize(source, 0));
						vec3 b = texture(source, uv - vec2(0.0, t.y)).rgb;
						vec3 d = texture(source, uv - vec2(t.x, 0.0)).rgb;
						vec3 e = texture(source, uv).rgb;
						vec3 f = texture(source, uv + vec2(t.x, 0.0)).rgb;
						vec3 h = texture(source, uv + vec2(0.0, t.y)).rgb;
						vec3 mn4 = min(min(b, d), min(f, h)), mx4 = max(max(b, d), max(f, h));
						vec3 hitMin = min(mn4, e) / max(4.0 * mx4, vec3(1.0 / 65536.0));
						vec3 hitMax = (1.0 - max(mx4, e)) / min(4.0 * mn4 - 4.0, vec3(-1.0 / 65536.0));
						vec3 lobeRGB = max(-hitMin, hitMax);
						// 0.25 - 1/16: FSR_RCAS_LIMIT, keeps the kernel from inverting
						float lobe = max(-0.1875, min(max(lobeRGB.r, max(lobeRGB.g, lobeRGB.b)), 0.0)) * SHARPNESS;
						color = vec4((lobe * (b + d + f + h) + e) / (4.0 * lobe + 1.0), 1.0);
					}
					""");
			uSource = GL20C.glGetUniformLocation(program, "source");
			uSize = GL20C.glGetUniformLocation(program, "size");
			vao = GL30C.glGenVertexArrays();
			ready = true;
			log.info("Render scale ready");
			return true;
		} catch (RuntimeException e) {
			failed = true;
			log.error("Render scale could not start", e);
			return false;
		}
	}

	private int program(String fragment) {
		int vs = shader(GL20C.GL_VERTEX_SHADER, """
				#version 150 core
				void main() {
					vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
					gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
				}
				""");
		int fs = shader(GL20C.GL_FRAGMENT_SHADER, fragment);
		int program = GL20C.glCreateProgram();
		GL20C.glAttachShader(program, vs);
		GL20C.glAttachShader(program, fs);
		GL20C.glLinkProgram(program);
		GL20C.glDeleteShader(vs);
		GL20C.glDeleteShader(fs);
		if (GL20C.glGetProgrami(program, GL20C.GL_LINK_STATUS) == 0) {
			throw new IllegalStateException(GL20C.glGetProgramInfoLog(program));
		}
		return program;
	}

	private int shader(int type, String source) {
		int shader = GL20C.glCreateShader(type);
		GL20C.glShaderSource(shader, source);
		GL20C.glCompileShader(shader);
		if (GL20C.glGetShaderi(shader, GL20C.GL_COMPILE_STATUS) == 0) {
			throw new IllegalStateException(GL20C.glGetShaderInfoLog(shader));
		}
		return shader;
	}
}
