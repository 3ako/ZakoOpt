package zako.opt.gl;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.LocalSectionIndex;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.util.iterator.ByteIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import zako.opt.DebugToggle;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL21C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL33C;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;

// terrain sections that sit wholly behind the opaque terrain of a recent frame are left out of Sodium's draws. After the
// opaque passes the GPU shrinks the depth buffer to one texel per 8x8 block, keeping the farthest depth of each block, and
// copies it out asynchronously; once a copy is ready the CPU projects each section's box onto it, so nothing ever waits on
// the GPU. A hidden verdict holds only while the camera stays within MOVE of where that depth was drawn
@Slf4j(topic = "zakoopt")
@UtilityClass
public class OcclusionCull {
	private final int SLOTS = 3;
	private final int BLOCK = 8;
	private final double NEAR = 24;
	private final double MOVE = 0.5;
	private final int EXPIRE = 6;
	private final int MAX_TEXELS = 4096;
	// whether culling pays here is measured, not guessed: the depth pass alone can cost an integrated GPU more than the culling
	// saves. Every RETRIAL_NS the frame time is sampled in on-off-on-off runs of TRIAL frames, and culling stays only if it wins
	private final int TRIAL = 40;
	private final long RETRIAL_NS = 20_000_000_000L;
	private final double MARGIN = 0.98;
	// -Dzakoopt.occlusion_cull.force=true: always on, for checking what it hides
	private final boolean FORCED = Boolean.getBoolean("zakoopt.occlusion_cull.force");
	private final TerrainRenderPass[] PASSES = {DefaultTerrainRenderPasses.SOLID, DefaultTerrainRenderPasses.CUTOUT, DefaultTerrainRenderPasses.TRANSLUCENT};

	private int program, vao, fbo, texture, uDepth, uBlock, uSize, uReversed;
	private int texWidth, texHeight;
	private boolean ready, failed, armed;
	private int frame;
	private long lastPassNs;
	private double camX, camY, camZ;
	private boolean worth = true;
	private int trialFrame = -1;
	private long trialEndNs, lastFrameNs;
	private final long[][] trialTimes = new long[2][TRIAL * 2];

	private final int[] pbos = new int[SLOTS];
	private final long[] fences = new long[SLOTS];
	private final int[] slotFrame = new int[SLOTS], slotWidth = new int[SLOTS], slotHeight = new int[SLOTS];
	private final Matrix4f[] slotMatrix = {new Matrix4f(), new Matrix4f(), new Matrix4f()};
	// 26.2 on keeps depth in 0..1 with near at 1 (reversed); older versions keep -1..1 with far at 1
	private final boolean[] slotReversed = new boolean[SLOTS], slotZeroToOne = new boolean[SLOTS];
	private final double[][] slotCamera = new double[SLOTS][3];

	// the newest depth copy the CPU holds
	private float[] depth = new float[0];
	private int depthWidth, depthHeight, depthFrame = Integer.MIN_VALUE / 2;
	private final Matrix4f depthMatrix = new Matrix4f();
	private boolean depthReversed, depthZeroToOne;
	private double depthX, depthY, depthZ;

	// value: frame << 1 | hidden, so each section is tested once a frame whichever pass asks first
	private final Long2IntOpenHashMap verdicts = new Long2IntOpenHashMap();
	private final Filter filter = new Filter();
	private final SectionRenderDataStorage[] storages = new SectionRenderDataStorage[PASSES.length];
	private final float[] corner = new float[4];

	private long sections, culled, weightAll, weightCulled, cpuNs, statFrames, windowStart;
	private String line = "Occlusion: collecting";

	static {
		verdicts.defaultReturnValue(-1);
		zako.opt.DebugInfo.EXTRA.add(() -> zako.opt.feature.Features.OCCLUSION_CULL.on ? java.util.List.of(line) : java.util.List.of());
	}

	public String line() {
		return line;
	}

	// prototype: -Dzakoopt.occlusion_cull.cpu=true takes the depth from CpuOcclusion instead of the GPU copy
	private boolean cpu;

	public void beginPass(TerrainRenderPass pass, CameraTransform camera, ChunkRenderMatrices matrices) {
		if (pass != DefaultTerrainRenderPasses.SOLID || failed) {
			return;
		}
		cpu = Boolean.getBoolean("zakoopt.occlusion_cull.cpu");
		long now = System.nanoTime();
		if (now - lastPassNs > 1_000_000_000L) {
			depthFrame = Integer.MIN_VALUE / 2;
		}
		trial(now);
		lastPassNs = now;
		frame++;
		camX = camera.x;
		camY = camera.y;
		camZ = camera.z;
		armed = true;
		if (cpu) {
			long start = System.nanoTime();
			if (culling()) {
				var target = MainView.target();
				boolean rev = reversed(matrices), z01 = zeroToOne();
				depthMatrix.set(matrices.projection()).mul(matrices.modelView());
				CpuOcclusion.build(depthMatrix, camX, camY, camZ, rev, z01, (float) target.width / target.height);
				depth = CpuOcclusion.depth;
				depthWidth = CpuOcclusion.width;
				depthHeight = CpuOcclusion.height;
				depthReversed = rev;
				depthZeroToOne = z01;
				depthX = camX;
				depthY = camY;
				depthZ = camZ;
				depthFrame = frame;
			}
			cpuNs += System.nanoTime() - start;
			return;
		}
		if (ready) {
			long start = System.nanoTime();
			collect();
			cpuNs += System.nanoTime() - start;
		}
	}

	public void endPass(ChunkRenderMatrices matrices, ChunkRenderListIterable lists, TerrainRenderPass pass, CameraTransform camera) {
		if (pass != DefaultTerrainRenderPasses.CUTOUT || !armed || failed) {
			return;
		}
		armed = false;
		if (cpu) {
			if (DebugToggle.visible()) {
				count(lists);
			}
			statFrames++;
			window();
			return;
		}
		if (GlTargets.depth(MainView.target()) < 0) {
			return;
		}
		if (!ready && !init()) {
			return;
		}
		long start = System.nanoTime();
		if (culling()) {
			capture(frame % SLOTS, matrices, camera);
		}
		if (DebugToggle.visible()) {
			count(lists);
		}
		cpuNs += System.nanoTime() - start;
		statFrames++;
		window();
	}

	// the sections Sodium is about to draw for one region, without the hidden ones
	public ByteIterator filter(ByteIterator sections, RenderRegion region) {
		if (sections == null || !ready && !cpu) {
			return sections;
		}
		filter.in = sections;
		filter.region = region;
		filter.advance();
		return filter;
	}

	private boolean hidden(long key, int cx, int cy, int cz) {
		int v = verdicts.get(key);
		if (v >= 0 && v >>> 1 == frame) {
			return (v & 1) != 0;
		}
		boolean hidden = test(cx, cy, cz);
		verdicts.put(key, frame << 1 | (hidden ? 1 : 0));
		return hidden;
	}

	// during a trial the runs go on, off, on, off; otherwise the last verdict holds
	private boolean culling() {
		if (FORCED) {
			return true;
		}
		return trialFrame < 0 ? worth : (trialFrame / TRIAL) % 2 == 0;
	}

	private void trial(long now) {
		long dt = now - lastFrameNs;
		lastFrameNs = now;
		if (trialFrame < 0) {
			if (now > trialEndNs + RETRIAL_NS) {
				trialFrame = 0;
			}
			return;
		}
		// a frame is charged to the run whose culling it drew with; the first frame of each run still uses the run before
		if (trialFrame % TRIAL != 0 && dt > 0 && dt < 200_000_000L) {
			int run = trialFrame / TRIAL;
			trialTimes[run % 2][(run / 2) * TRIAL + trialFrame % TRIAL] = dt;
		}
		if (++trialFrame == TRIAL * 4) {
			long on = median(trialTimes[0]), off = median(trialTimes[1]);
			worth = on < off * MARGIN;
			log.info("Occlusion culling {}: {} us a frame with it, {} us without", worth ? "on" : "off", on / 1000, off / 1000);
			trialFrame = -1;
			trialEndNs = now;
		}
	}

	private long median(long[] times) {
		long[] sorted = java.util.Arrays.stream(times).filter(t -> t > 0).sorted().toArray();
		java.util.Arrays.fill(times, 0);
		return sorted.length == 0 ? Long.MAX_VALUE : sorted[sorted.length / 2];
	}

	private boolean usable() {
		double mx = camX - depthX, my = camY - depthY, mz = camZ - depthZ;
		return frame - depthFrame <= EXPIRE && mx * mx + my * my + mz * mz <= MOVE * MOVE;
	}

	private boolean test(int cx, int cy, int cz) {
		// measurement only: everything well under the camera counts as hidden, the most culling underground could ever give
		if (cpu && Boolean.getBoolean("zakoopt.occlusion_cull.ceiling")) {
			return (cy << 4) + 16 < camY - 24;
		}
		if (!usable()) {
			return false;
		}
		int x = cx << 4, y = cy << 4, z = cz << 4;
		double dx = Math.max(Math.max(x - camX, camX - x - 16), 0);
		double dy = Math.max(Math.max(y - camY, camY - y - 16), 0);
		double dz = Math.max(Math.max(z - camZ, camZ - z - 16), 0);
		if (dx * dx + dy * dy + dz * dz < NEAR * NEAR) {
			return false;
		}
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, nearest = depthReversed ? -Float.MAX_VALUE : Float.MAX_VALUE;
		for (int i = 0; i < 8; i++) {
			// one block of margin: models may stick out of their section a little
			float px = (float) (x - 1 + ((i & 1) == 0 ? 0 : 18) - depthX);
			float py = (float) (y - 1 + ((i & 2) == 0 ? 0 : 18) - depthY);
			float pz = (float) (z - 1 + ((i & 4) == 0 ? 0 : 18) - depthZ);
			Matrix4f m = depthMatrix;
			float w = m.m03() * px + m.m13() * py + m.m23() * pz + m.m33();
			if (w < 0.05f) {
				return false;
			}
			float nx = (m.m00() * px + m.m10() * py + m.m20() * pz + m.m30()) / w;
			float ny = (m.m01() * px + m.m11() * py + m.m21() * pz + m.m31()) / w;
			float nz = (m.m02() * px + m.m12() * py + m.m22() * pz + m.m32()) / w;
			minX = Math.min(minX, nx);
			maxX = Math.max(maxX, nx);
			minY = Math.min(minY, ny);
			maxY = Math.max(maxY, ny);
			nearest = depthReversed ? Math.max(nearest, nz) : Math.min(nearest, nz);
		}
		// only the part on the screen can be seen, so only that part has to be covered
		if (maxX < -1 || minX > 1 || maxY < -1 || minY > 1) {
			return false;
		}
		minX = Math.max(minX, -1);
		maxX = Math.min(maxX, 1);
		minY = Math.max(minY, -1);
		maxY = Math.min(maxY, 1);
		int x0 = (int) ((minX * 0.5f + 0.5f) * depthWidth), x1 = Math.min(depthWidth - 1, (int) ((maxX * 0.5f + 0.5f) * depthWidth));
		int y0 = (int) ((minY * 0.5f + 0.5f) * depthHeight), y1 = Math.min(depthHeight - 1, (int) ((maxY * 0.5f + 0.5f) * depthHeight));
		if ((x1 - x0 + 1) * (y1 - y0 + 1) > (cpu ? depthWidth * depthHeight : MAX_TEXELS)) {
			return false;
		}
		float boxDepth = depthZeroToOne ? nearest : nearest * 0.5f + 0.5f;
		for (int ty = y0; ty <= y1; ty++) {
			int row = ty * depthWidth;
			for (int tx = x0; tx <= x1; tx++) {
				if (depthReversed ? depth[row + tx] <= boxDepth : depth[row + tx] >= boxDepth) {
					return false;
				}
			}
		}
		return true;
	}

	// the newest finished copy, without waiting for one that is not
	private void collect() {
		int newest = -1;
		for (int s = 0; s < SLOTS; s++) {
			if (fences[s] != 0 && GL32C.glClientWaitSync(fences[s], 0, 0) != GL32C.GL_TIMEOUT_EXPIRED && (newest < 0 || slotFrame[s] > slotFrame[newest])) {
				newest = s;
			}
		}
		if (newest < 0) {
			return;
		}
		for (int s = 0; s < SLOTS; s++) {
			if (fences[s] != 0 && slotFrame[s] <= slotFrame[newest]) {
				GL32C.glDeleteSync(fences[s]);
				fences[s] = 0;
			}
		}
		int w = slotWidth[newest], h = slotHeight[newest];
		if (depth.length < w * h) {
			depth = new float[w * h];
		}
		int prev = GL11C.glGetInteger(GL21C.GL_PIXEL_PACK_BUFFER_BINDING);
		GL15C.glBindBuffer(GL21C.GL_PIXEL_PACK_BUFFER, pbos[newest]);
		ByteBuffer mapped = GL30C.glMapBufferRange(GL21C.GL_PIXEL_PACK_BUFFER, 0, (long) w * h * 4, GL30C.GL_MAP_READ_BIT);
		if (mapped != null) {
			mapped.order(ByteOrder.nativeOrder()).asFloatBuffer().get(depth, 0, w * h);
			GL15C.glUnmapBuffer(GL21C.GL_PIXEL_PACK_BUFFER);
			depthWidth = w;
			depthHeight = h;
			depthFrame = slotFrame[newest];
			depthMatrix.set(slotMatrix[newest]);
			depthReversed = slotReversed[newest];
			depthZeroToOne = slotZeroToOne[newest];
			depthX = slotCamera[newest][0];
			depthY = slotCamera[newest][1];
			depthZ = slotCamera[newest][2];
		}
		GL15C.glBindBuffer(GL21C.GL_PIXEL_PACK_BUFFER, prev);
	}

	private void capture(int s, ChunkRenderMatrices matrices, CameraTransform camera) {
		if (fences[s] != 0) {
			return;
		}
		GpuTimers.mark("occlusion depth");
		var target = MainView.target();
		int srcW = target.width, srcH = target.height;
		int w = (srcW + BLOCK - 1) / BLOCK, h = (srcH + BLOCK - 1) / BLOCK;

		int prevDraw = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING), prevRead = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
		int prevProgram = GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM), prevVao = GL11C.glGetInteger(GL30C.GL_VERTEX_ARRAY_BINDING);
		int prevUnit = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
		GL13C.glActiveTexture(GL13C.GL_TEXTURE0);
		int prevTexture = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
		int prevPack = GL11C.glGetInteger(GL21C.GL_PIXEL_PACK_BUFFER_BINDING);
		int[] viewport = new int[4];
		GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, viewport);
		boolean depthTest = GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST), blend = GL11C.glIsEnabled(GL11C.GL_BLEND), cull = GL11C.glIsEnabled(GL11C.GL_CULL_FACE),
				scissor = GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST);
		boolean depthMask = GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK);

		if (w != texWidth || h != texHeight) {
			texWidth = w;
			texHeight = h;
			GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, texture);
			GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL30C.GL_R32F, w, h, 0, GL11C.GL_RED, GL11C.GL_FLOAT, (ByteBuffer) null);
			for (int p = 0; p < SLOTS; p++) {
				GL15C.glBindBuffer(GL21C.GL_PIXEL_PACK_BUFFER, pbos[p]);
				GL15C.glBufferData(GL21C.GL_PIXEL_PACK_BUFFER, (long) w * h * 4, GL15C.GL_STREAM_READ);
			}
		}

		GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, fbo);
		GL11C.glViewport(0, 0, w, h);
		GL11C.glDisable(GL11C.GL_DEPTH_TEST);
		GL11C.glDisable(GL11C.GL_BLEND);
		GL11C.glDisable(GL11C.GL_CULL_FACE);
		GL11C.glDisable(GL11C.GL_SCISSOR_TEST);
		GL11C.glDepthMask(false);
		GL20C.glUseProgram(program);
		GL30C.glBindVertexArray(vao);
		// blaze3d binds sampler objects to the units, and one left on unit 0 would override the texture's own sampling
		int prevSampler = GlTargets.SAMPLERS ? GL11C.glGetInteger(GL33C.GL_SAMPLER_BINDING) : 0;
		if (GlTargets.SAMPLERS) {
			GL33C.glBindSampler(0, 0);
		}
		GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, GlTargets.depth(target));
		GL20C.glUniform1i(uDepth, 0);
		GL20C.glUniform2i(uBlock, BLOCK, BLOCK);
		GL20C.glUniform2i(uSize, srcW, srcH);
		boolean reversed = reversed(matrices);
		GL20C.glUniform1i(uReversed, reversed ? 1 : 0);
		GL11C.glDrawArrays(GL11C.GL_TRIANGLES, 0, 3);

		GL15C.glBindBuffer(GL21C.GL_PIXEL_PACK_BUFFER, pbos[s]);
		// texture downloads elsewhere may leave a row length or skips set, which would overrun the buffer
		int rowLength = GL11C.glGetInteger(GL11C.GL_PACK_ROW_LENGTH), skipRows = GL11C.glGetInteger(GL11C.GL_PACK_SKIP_ROWS),
				skipPixels = GL11C.glGetInteger(GL11C.GL_PACK_SKIP_PIXELS), alignment = GL11C.glGetInteger(GL11C.GL_PACK_ALIGNMENT);
		GL11C.glPixelStorei(GL11C.GL_PACK_ROW_LENGTH, 0);
		GL11C.glPixelStorei(GL11C.GL_PACK_SKIP_ROWS, 0);
		GL11C.glPixelStorei(GL11C.GL_PACK_SKIP_PIXELS, 0);
		GL11C.glPixelStorei(GL11C.GL_PACK_ALIGNMENT, 4);
		GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, fbo);
		GL11C.glReadBuffer(GL30C.GL_COLOR_ATTACHMENT0);
		GL11C.glReadPixels(0, 0, w, h, GL11C.GL_RED, GL11C.GL_FLOAT, 0);
		GL11C.glPixelStorei(GL11C.GL_PACK_ROW_LENGTH, rowLength);
		GL11C.glPixelStorei(GL11C.GL_PACK_SKIP_ROWS, skipRows);
		GL11C.glPixelStorei(GL11C.GL_PACK_SKIP_PIXELS, skipPixels);
		GL11C.glPixelStorei(GL11C.GL_PACK_ALIGNMENT, alignment);
		fences[s] = GL32C.glFenceSync(GL32C.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
		slotFrame[s] = frame;
		slotWidth[s] = w;
		slotHeight[s] = h;
		slotMatrix[s].set(matrices.projection()).mul(matrices.modelView());
		slotReversed[s] = reversed;
		slotZeroToOne[s] = zeroToOne();
		slotCamera[s][0] = camera.x;
		slotCamera[s][1] = camera.y;
		slotCamera[s][2] = camera.z;

		GL15C.glBindBuffer(GL21C.GL_PIXEL_PACK_BUFFER, prevPack);
		GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, prevDraw);
		GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, prevRead);
		GL11C.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
		GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, prevTexture);
		if (GlTargets.SAMPLERS) {
			GL33C.glBindSampler(0, prevSampler);
		}
		GL13C.glActiveTexture(prevUnit);
		GL30C.glBindVertexArray(prevVao);
		GL20C.glUseProgram(prevProgram);
		GL11C.glDepthMask(depthMask);
		if (depthTest) {
			GL11C.glEnable(GL11C.GL_DEPTH_TEST);
		}
		if (blend) {
			GL11C.glEnable(GL11C.GL_BLEND);
		}
		if (cull) {
			GL11C.glEnable(GL11C.GL_CULL_FACE);
		}
		if (scissor) {
			GL11C.glEnable(GL11C.GL_SCISSOR_TEST);
		}
		GpuTimers.mark("entities, block entities");
	}

	private void count(ChunkRenderListIterable lists) {
		Iterator<ChunkRenderList> it = lists.iterator(false);
		while (it.hasNext()) {
			ChunkRenderList list = it.next();
			RenderRegion region = list.getRegion();
			ByteIterator iterator = list.sectionsWithGeometryIterator(false);
			if (iterator == null) {
				continue;
			}
			for (int p = 0; p < PASSES.length; p++) {
				storages[p] = region.getStorage(PASSES[p]);
			}
			while (iterator.hasNext()) {
				int local = iterator.nextByteAsInt();
				int cx = region.getChunkX() + LocalSectionIndex.unpackX(local);
				int cy = region.getChunkY() + LocalSectionIndex.unpackY(local);
				int cz = region.getChunkZ() + LocalSectionIndex.unpackZ(local);
				long weight = weight(local);
				sections++;
				weightAll += weight;
				if (hidden(SectionPos.asLong(cx, cy, cz), cx, cy, cz)) {
					culled++;
					weightCulled += weight;
				}
			}
		}
	}

	private long weight(int local) {
		long elements = 0;
		for (SectionRenderDataStorage storage : storages) {
			if (storage == null) {
				continue;
			}
			long data = storage.getDataPointer(local);
			for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
				elements += SodiumSections.count(data, facing);
			}
		}
		return elements;
	}

	private void window() {
		long now = System.nanoTime();
		if (windowStart == 0) {
			windowStart = now;
			return;
		}
		if (now - windowStart < 1_000_000_000L || statFrames == 0) {
			return;
		}
		line = "Occlusion%s%s: %d%% of triangles culled (%d of %d sections); %.2f ms CPU; depth %d frames old".formatted(
				cpu ? " CPU[groups %d planes %d missing %d covered %d]".formatted(CpuOcclusion.groups, CpuOcclusion.planes, CpuOcclusion.missing, CpuOcclusion.covered) : "", FORCED ? " (forced)" : trialFrame >= 0 ? " (measuring)" : worth ? "" : " (off: slower here)",
				weightAll == 0 ? 0 : weightCulled * 100 / weightAll, culled / statFrames, sections / statFrames, cpuNs / 1e6 / statFrames, frame - depthFrame);
		sections = culled = weightAll = weightCulled = cpuNs = statFrames = 0;
		windowStart = now;
		if (verdicts.size() > 65536) {
			verdicts.clear();
		}
	}

	// depth falls with distance: a point far down the view axis lands below a near one
	private boolean reversed(ChunkRenderMatrices matrices) {
		var p = matrices.projection();
		float near = (p.m22() * -1 + p.m32()) / (p.m23() * -1 + p.m33()), far = (p.m22() * -1000 + p.m32()) / (p.m23() * -1000 + p.m33());
		return far < near;
	}

	private boolean zeroToOne() {
		org.lwjgl.opengl.GLCapabilities caps;
		try {
			caps = org.lwjgl.opengl.GL.getCapabilities();
		} catch (IllegalStateException e) {
			// no GL context: the Vulkan backend, whose depth is always 0..1
			return true;
		}
		return (caps.OpenGL45 || caps.GL_ARB_clip_control) && GL11C.glGetInteger(org.lwjgl.opengl.GL45C.GL_CLIP_DEPTH_MODE) == org.lwjgl.opengl.GL45C.GL_ZERO_TO_ONE;
	}

	private boolean init() {
		try {
			int vs = shader(GL20C.GL_VERTEX_SHADER, """
					#version 150 core
					void main() {
						vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
						gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
					}
					""");
			int fs = shader(GL20C.GL_FRAGMENT_SHADER, """
					#version 150 core
					uniform sampler2D depth;
					uniform ivec2 block;
					uniform ivec2 size;
					uniform bool reversed;
					out vec4 color;
					void main() {
						ivec2 origin = ivec2(gl_FragCoord.xy) * block;
						float far = reversed ? 1.0 : 0.0;
						for (int y = 0; y < block.y; y++) {
							for (int x = 0; x < block.x; x++) {
								float d = texelFetch(depth, min(origin + ivec2(x, y), size - 1), 0).r;
								far = reversed ? min(far, d) : max(far, d);
							}
						}
						color = vec4(far);
					}
					""");
			program = GL20C.glCreateProgram();
			GL20C.glAttachShader(program, vs);
			GL20C.glAttachShader(program, fs);
			GL20C.glLinkProgram(program);
			GL20C.glDeleteShader(vs);
			GL20C.glDeleteShader(fs);
			if (GL20C.glGetProgrami(program, GL20C.GL_LINK_STATUS) == 0) {
				throw new IllegalStateException(GL20C.glGetProgramInfoLog(program));
			}
			uDepth = GL20C.glGetUniformLocation(program, "depth");
			uBlock = GL20C.glGetUniformLocation(program, "block");
			uSize = GL20C.glGetUniformLocation(program, "size");
			uReversed = GL20C.glGetUniformLocation(program, "reversed");
			vao = GL30C.glGenVertexArrays();

			int prevTexture = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
			int prevFbo = GL11C.glGetInteger(GL30C.GL_FRAMEBUFFER_BINDING);
			texture = GL11C.glGenTextures();
			GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, texture);
			GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST);
			GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST);
			GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL30C.GL_R32F, 1, 1, 0, GL11C.GL_RED, GL11C.GL_FLOAT, (ByteBuffer) null);
			fbo = GL30C.glGenFramebuffers();
			GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, fbo);
			GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, GL11C.GL_TEXTURE_2D, texture, 0);
			GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, prevFbo);
			GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, prevTexture);
			GL15C.glGenBuffers(pbos);
			texWidth = texHeight = -1;
			ready = true;
			log.info("Occlusion culling ready");
			return true;
		} catch (RuntimeException e) {
			failed = true;
			log.error("Occlusion culling could not start", e);
			return false;
		}
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

	// one instance for every region: Sodium drains the iterator before it asks for the next region's
	private final class Filter implements ByteIterator {
		ByteIterator in;
		RenderRegion region;
		int next = -1;

		void advance() {
			next = -1;
			while (in.hasNext()) {
				int local = in.nextByteAsInt();
				int cx = region.getChunkX() + LocalSectionIndex.unpackX(local);
				int cy = region.getChunkY() + LocalSectionIndex.unpackY(local);
				int cz = region.getChunkZ() + LocalSectionIndex.unpackZ(local);
				if (!hidden(SectionPos.asLong(cx, cy, cz), cx, cy, cz)) {
					next = local;
					return;
				}
			}
		}

		@Override
		public boolean hasNext() {
			return next >= 0;
		}

		@Override
		public int nextByteAsInt() {
			int local = next;
			advance();
			return local;
		}
	}
}
