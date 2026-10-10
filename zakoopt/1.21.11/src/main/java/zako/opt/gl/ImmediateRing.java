package zako.opt.gl;

import org.lwjgl.opengl.GL11C;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlFence;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;
import zako.opt.ZakoOptConfig;
import zako.opt.mixin.gl.GlBufferInvoker;

@UtilityClass
@Slf4j(topic = "zakoopt")
public class ImmediateRing {
	private final int SEGMENTS = 3;
	private final int INITIAL_SEGMENT = 4 << 20;
	private final int MAX_SEGMENT = 128 << 20;
	private final Map<VertexFormat, Ring> RINGS = new IdentityHashMap<>();
	private final GpuFence[] FENCES = new GpuFence[SEGMENTS];
	private long frame;
	private boolean usedThisFrame;
	private boolean coherentChecked;
	private DirectStateAccess dsa;
	private int liveBuilders;

	private final class Ring {
		GpuBuffer buffer;
		ByteBuffer coherent;
		int segmentSize;
		int cursor;
		int needed;
	}

	// returns the base vertex inside buffer(format), or -1 to fall back to the vanilla upload
	public int upload(VertexFormat format, ByteBuffer data) {
		if (!ZakoOptConfig.immediateRing()) {
			return -1;
		}
		Ring ring = ring(format);
		int stride = format.getVertexSize();
		int length = data.remaining();
		if (ring.coherent != null) {
			long base = MemoryUtil.memAddress0(ring.coherent);
			long address = MemoryUtil.memAddress(data);
			if (address >= base && address + length <= base + ring.coherent.capacity()) {
				// written in place by a ring-backed builder: nothing to copy
				usedThisFrame = true;
				return (int) ((address - base) / stride);
			}
		}
		int offset = reserve(ring, stride, length);
		if (offset < 0) {
			return -1;
		}
		if (ring.coherent != null) {
			// coherent persistent mapping: the GPU sees these bytes without any flush call
			MemoryUtil.memCopy(MemoryUtil.memAddress(data), MemoryUtil.memAddress0(ring.coherent) + offset, length);
		} else {
			CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
			try (GpuBuffer.MappedView view = encoder.mapBuffer(ring.buffer.slice(offset, length), false, true)) {
				MemoryUtil.memCopy(MemoryUtil.memAddress(data), MemoryUtil.memAddress(view.data()), length);
			}
		}
		usedThisFrame = true;
		return offset / stride;
	}

	// memory for a vertex builder inside this frame's segment, so its draw needs no copy; 0 = use normal memory
	public long reserveBuilder(VertexFormat format, int bytes) {
		if (!ZakoOptConfig.immediateRing() || !ZakoOptConfig.ringZeroCopy()) {
			return 0;
		}
		Ring ring = ring(format);
		if (ring.coherent == null) {
			return 0;
		}
		int offset = reserve(ring, format.getVertexSize(), bytes);
		if (offset < 0) {
			return 0;
		}
		usedThisFrame = true;
		liveBuilders++;
		return MemoryUtil.memAddress0(ring.coherent) + offset;
	}

	public void builderReleased() {
		liveBuilders--;
	}

	private Ring ring(VertexFormat format) {
		Ring ring = RINGS.computeIfAbsent(format, f -> new Ring());
		if (ring.buffer == null) {
			allocate(ring, format, INITIAL_SEGMENT);
			ring.cursor = segmentStart(ring);
		}
		return ring;
	}

	private int reserve(Ring ring, int stride, int length) {
		int offset = (ring.cursor + stride - 1) / stride * stride;
		if (offset + length > segmentStart(ring) + ring.segmentSize) {
			ring.needed = Math.max(ring.needed, offset - segmentStart(ring) + length);
			return -1;
		}
		ring.cursor = offset + length;
		return offset;
	}

	public GpuBuffer buffer(VertexFormat format) {
		return RINGS.get(format).buffer;
	}

	// called once per frame right before the swap; fences live for 3 frames, so no try-with-resources
	@SuppressWarnings("resource")
	public void endFrame() {
		if (!usedThisFrame && RINGS.isEmpty()) {
			return;
		}
		int segment = (int) (frame % SEGMENTS);
		if (FENCES[segment] != null) {
			FENCES[segment].close();
		}
		// shares the frame's single fence; must run before FrameFence.endFrame
		FENCES[segment] = FrameFence.enabled() ? FrameFence.create() : new GlFence();
		usedThisFrame = false;
		frame++;
		int next = (int) (frame % SEGMENTS);
		if (FENCES[next] != null) {
			// GPU must be done with the frame that used this segment 3 frames ago (it nearly always is)
			FENCES[next].awaitCompletion(Long.MAX_VALUE);
			FENCES[next].close();
			FENCES[next] = null;
		}
		for (Map.Entry<VertexFormat, Ring> e : RINGS.entrySet()) {
			Ring ring = e.getValue();
			if (ring.needed > 0 && liveBuilders == 0) {
				waitAll();
				ring.buffer.close();
				int size = Math.min(MAX_SEGMENT, Integer.highestOneBit(ring.needed) << 1);
				allocate(ring, e.getKey(), size);
				log.info("Immediate ring for {} grown to {} KB per frame", e.getKey(), size >> 10);
				ring.needed = 0;
			}
			ring.cursor = segmentStart(ring);
		}
	}

	private int segmentStart(Ring ring) {
		return (int) (frame % SEGMENTS) * ring.segmentSize;
	}

	// resolved once on the render thread: GL capabilities need a current context
	private boolean coherentSupported() {
		if (coherentChecked) {
			return dsa != null;
		}
		coherentChecked = true;
		GLCapabilities caps = GL.getCapabilities();
		if (RenderSystem.getDevice() instanceof GlDevice device
				&& (caps.OpenGL45 || caps.GL_ARB_direct_state_access) && (caps.OpenGL44 || caps.GL_ARB_buffer_storage)) {
			dsa = device.directStateAccess();
		}
		log.info("Immediate ring: {}", dsa != null ? "coherent persistent mapping" : "explicit flush fallback");
		return dsa != null;
	}

	private void allocate(Ring ring, VertexFormat format, int segmentSize) {
		ring.segmentSize = segmentSize;
		long size = (long) segmentSize * SEGMENTS;
		Supplier<String> label = () -> "zakoopt immediate ring " + format;
		ring.coherent = null;
		if (coherentSupported()) {
			int flags = GL30C.GL_MAP_WRITE_BIT | ARBBufferStorage.GL_MAP_PERSISTENT_BIT | ARBBufferStorage.GL_MAP_COHERENT_BIT;
			int handle = ARBDirectStateAccess.glCreateBuffers();
			ARBDirectStateAccess.glNamedBufferStorage(handle, size, flags);
			ByteBuffer mapped = ARBDirectStateAccess.glMapNamedBufferRange(handle, 0, size, flags);
			if (mapped != null) {
				ring.coherent = mapped;
				ring.buffer = GlBufferInvoker.zakoopt$create(label, dsa, GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, size, handle, mapped);
				return;
			}
			GL15C.glDeleteBuffers(handle);
		}
		ring.buffer = RenderSystem.getDevice().createBuffer(label, GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, size);
	}

	private void waitAll() {
		// this frame's fence was created moments ago; without a flush the driver may never signal it (Intel does not)
		GL11C.glFlush();
		for (int i = 0; i < SEGMENTS; i++) {
			if (FENCES[i] != null) {
				FENCES[i].awaitCompletion(Long.MAX_VALUE);
				FENCES[i].close();
				FENCES[i] = null;
			}
		}
	}
}
