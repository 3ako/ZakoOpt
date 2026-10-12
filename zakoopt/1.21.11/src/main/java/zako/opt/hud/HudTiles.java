package zako.opt.hud;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL33C;

import java.nio.ByteBuffer;

// most of the cached HUD texture is empty, yet blending it in reads and writes every pixel of the screen. When the HUD is
// redrawn, a mask marks the 32x32 tiles that hold anything; each frame only those tiles are blended in, the empty ones
// collapse to nothing in the vertex shader. The mask stays on the GPU, so it is never a frame late
@Slf4j(topic = "zakoopt")
@UtilityClass
public class HudTiles {
	private final int TILE = 32;

	private int maskProgram, blendProgram, vao, maskFbo, maskTexture, mainFbo;
	private int uMaskHud, uMaskSize, uTiles, uScreen, uMask, uHud;
	private int tilesX, tilesY;
	private boolean ready, failed;

	// after the HUD was drawn into the texture
	public void mark(RenderTarget hud) {
		if (failed || !ready && !init()) {
			return;
		}
		int w = hud.width, h = hud.height, tx = (w + TILE - 1) / TILE, ty = (h + TILE - 1) / TILE;
		State state = State.save();
		if (tx != tilesX || ty != tilesY) {
			tilesX = tx;
			tilesY = ty;
			GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, maskTexture);
			GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL30C.GL_R8, tx, ty, 0, GL11C.GL_RED, GL11C.GL_UNSIGNED_BYTE, (ByteBuffer) null);
		}
		GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, maskFbo);
		GL11C.glViewport(0, 0, tx, ty);
		GL11C.glDisable(GL11C.GL_BLEND);
		GL20C.glUseProgram(maskProgram);
		GL30C.glBindVertexArray(vao);
		GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, id(hud));
		GL20C.glUniform1i(uMaskHud, 0);
		GL20C.glUniform2i(uMaskSize, w, h);
		GL11C.glDrawArrays(GL11C.GL_TRIANGLES, 0, 3);
		state.restore();
	}

	// blends the marked tiles of the premultiplied HUD over the main target; false when it cannot, so the caller does it whole
	public boolean blend(RenderTarget hud, RenderTarget main) {
		if (!ready || tilesX == 0) {
			return false;
		}
		State state = State.save();
		if (mainFbo == 0) {
			mainFbo = GL30C.glGenFramebuffers();
		}
		GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, mainFbo);
		// attached every time: a resize deletes the texture, and GL may hand its name to the new one while the old stays attached here
		GL30C.glFramebufferTexture2D(GL30C.GL_DRAW_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, GL11C.GL_TEXTURE_2D, id(main), 0);
		GL11C.glViewport(0, 0, main.width, main.height);
		GL11C.glEnable(GL11C.GL_BLEND);
		GL14C.glBlendFuncSeparate(GL11C.GL_ONE, GL11C.GL_ONE_MINUS_SRC_ALPHA, GL11C.GL_ONE, GL11C.GL_ONE_MINUS_SRC_ALPHA);
		GL20C.glUseProgram(blendProgram);
		GL30C.glBindVertexArray(vao);
		GL13C.glActiveTexture(GL13C.GL_TEXTURE1);
		GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, maskTexture);
		GL13C.glActiveTexture(GL13C.GL_TEXTURE0);
		GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, id(hud));
		GL20C.glUniform1i(uHud, 0);
		GL20C.glUniform1i(uMask, 1);
		GL20C.glUniform2i(uTiles, tilesX, tilesY);
		GL20C.glUniform2f(uScreen, main.width, main.height);
		GL11C.glDrawArrays(GL11C.GL_TRIANGLES, 0, tilesX * tilesY * 6);
		GL13C.glActiveTexture(GL13C.GL_TEXTURE1);
		GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, state.texture1);
		state.restore();
		return true;
	}

	private int id(RenderTarget target) {
		return ((GlTexture) target.getColorTexture()).glId();
	}

	// blaze3d binds sampler objects to the units; they come off for these passes and go back after
	private record State(int program, int vao, int fbo, int unit, int texture0, int texture1, int sampler0, int sampler1, int[] viewport, boolean blend,
						 boolean depth, boolean cull, boolean scissor, int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
		static State save() {
			int[] viewport = new int[4];
			GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, viewport);
			int unit = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
			GL13C.glActiveTexture(GL13C.GL_TEXTURE1);
			int texture1 = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
			GL13C.glActiveTexture(GL13C.GL_TEXTURE0);
			int texture0 = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
			GL13C.glActiveTexture(GL13C.GL_TEXTURE1);
			int sampler1 = GL11C.glGetInteger(GL33C.GL_SAMPLER_BINDING);
			GL13C.glActiveTexture(GL13C.GL_TEXTURE0);
			int sampler0 = GL11C.glGetInteger(GL33C.GL_SAMPLER_BINDING);
			GL33C.glBindSampler(0, 0);
			GL33C.glBindSampler(1, 0);
			State s = new State(GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM), GL11C.glGetInteger(GL30C.GL_VERTEX_ARRAY_BINDING),
					GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING), unit, texture0, texture1, sampler0, sampler1, viewport, GL11C.glIsEnabled(GL11C.GL_BLEND),
					GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST), GL11C.glIsEnabled(GL11C.GL_CULL_FACE), GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST),
					GL11C.glGetInteger(GL14C.GL_BLEND_SRC_RGB), GL11C.glGetInteger(GL14C.GL_BLEND_DST_RGB), GL11C.glGetInteger(GL14C.GL_BLEND_SRC_ALPHA),
					GL11C.glGetInteger(GL14C.GL_BLEND_DST_ALPHA));
			GL11C.glDisable(GL11C.GL_DEPTH_TEST);
			GL11C.glDisable(GL11C.GL_CULL_FACE);
			GL11C.glDisable(GL11C.GL_SCISSOR_TEST);
			return s;
		}

		void restore() {
			GL33C.glBindSampler(0, sampler0);
			GL33C.glBindSampler(1, sampler1);
			GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, fbo);
			GL11C.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
			GL13C.glActiveTexture(GL13C.GL_TEXTURE0);
			GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, texture0);
			GL13C.glActiveTexture(unit);
			GL30C.glBindVertexArray(vao);
			GL20C.glUseProgram(program);
			GL14C.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
			toggle(GL11C.GL_BLEND, blend);
			toggle(GL11C.GL_DEPTH_TEST, depth);
			toggle(GL11C.GL_CULL_FACE, cull);
			toggle(GL11C.GL_SCISSOR_TEST, scissor);
		}

		private static void toggle(int cap, boolean on) {
			if (on) {
				GL11C.glEnable(cap);
			} else {
				GL11C.glDisable(cap);
			}
		}
	}

	private boolean init() {
		try {
			maskProgram = program("""
					#version 150 core
					void main() {
						vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
						gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
					}
					""", """
					#version 150 core
					uniform sampler2D hud;
					uniform ivec2 size;
					out vec4 color;
					void main() {
						ivec2 origin = ivec2(gl_FragCoord.xy) * 32;
						float any = 0.0;
						for (int y = 0; y < 32; y++) {
							for (int x = 0; x < 32; x++) {
								vec4 c = texelFetch(hud, min(origin + ivec2(x, y), size - 1), 0);
								any = max(any, max(max(c.r, c.g), max(c.b, c.a)));
							}
						}
						color = vec4(any > 0.0 ? 1.0 : 0.0);
					}
					""");
			blendProgram = program("""
					#version 150 core
					uniform sampler2D mask;
					uniform ivec2 tiles;
					uniform vec2 screen;
					const ivec2 corners[6] = ivec2[](ivec2(0, 0), ivec2(1, 0), ivec2(1, 1), ivec2(0, 0), ivec2(1, 1), ivec2(0, 1));
					void main() {
						int t = gl_VertexID / 6;
						ivec2 tile = ivec2(t % tiles.x, t / tiles.x);
						vec2 p = min(vec2((tile + corners[gl_VertexID % 6]) * 32), screen);
						gl_Position = texelFetch(mask, tile, 0).r > 0.0 ? vec4(p / screen * 2.0 - 1.0, 0.0, 1.0) : vec4(0.0);
					}
					""", """
					#version 150 core
					uniform sampler2D hud;
					out vec4 color;
					void main() {
						color = texelFetch(hud, ivec2(gl_FragCoord.xy), 0);
					}
					""");
			uMaskHud = GL20C.glGetUniformLocation(maskProgram, "hud");
			uMaskSize = GL20C.glGetUniformLocation(maskProgram, "size");
			uHud = GL20C.glGetUniformLocation(blendProgram, "hud");
			uMask = GL20C.glGetUniformLocation(blendProgram, "mask");
			uTiles = GL20C.glGetUniformLocation(blendProgram, "tiles");
			uScreen = GL20C.glGetUniformLocation(blendProgram, "screen");
			vao = GL30C.glGenVertexArrays();

			int prevTexture = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D), prevFbo = GL11C.glGetInteger(GL30C.GL_FRAMEBUFFER_BINDING);
			maskTexture = GL11C.glGenTextures();
			GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, maskTexture);
			GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST);
			GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST);
			GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL30C.GL_R8, 1, 1, 0, GL11C.GL_RED, GL11C.GL_UNSIGNED_BYTE, (ByteBuffer) null);
			maskFbo = GL30C.glGenFramebuffers();
			GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, maskFbo);
			GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, GL11C.GL_TEXTURE_2D, maskTexture, 0);
			GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, prevFbo);
			GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, prevTexture);
			ready = true;
			return true;
		} catch (RuntimeException e) {
			failed = true;
			log.error("HUD tiles could not start, the HUD cache blends the whole screen", e);
			return false;
		}
	}

	private int program(String vertex, String fragment) {
		int vs = shader(GL20C.GL_VERTEX_SHADER, vertex), fs = shader(GL20C.GL_FRAGMENT_SHADER, fragment);
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
