package zako.opt.gl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.Map;
import zako.opt.ZakoOptConfig;

// immediate draws from one persistently mapped buffer per vertex format instead of a glBufferData per draw;
// three per-frame segments, each reused only after the GPU signalled the frame that last used it
@UtilityClass
@Slf4j(topic = "zakoopt")
public class ImmediateRing {
	private final int SEGMENTS = 3;
	private final int INITIAL_SEGMENT = 4 << 20;
	private final int MAX_SEGMENT = 128 << 20;
	private final int FLAGS = GL30C.GL_MAP_WRITE_BIT | ARBBufferStorage.GL_MAP_PERSISTENT_BIT | ARBBufferStorage.GL_MAP_COHERENT_BIT;
	private final Map<VertexFormat, Ring> RINGS = new IdentityHashMap<>();
	private final long[] FENCES = new long[SEGMENTS];
	private long frame;
	private Boolean supported;
	private int liveBuilders;

	private final class Ring {
		int vao;
		int buffer;
		ByteBuffer mapped;
		int segmentSize;
		int cursor;
		int needed;
	}

	// false = draw it the vanilla way
	public boolean draw(MeshData mesh, boolean withShader) {
		if (!ZakoOptConfig.immediateRing() || !supported() || mesh.indexBuffer() != null) {
			return false;
		}
		CompiledShaderProgram shader = withShader ? RenderSystem.getShader() : null;
		if (withShader && shader == null) {
			return false;
		}
		MeshData.DrawState state = mesh.drawState();
		VertexFormat format = state.format();
		Ring ring = ring(format);
		ByteBuffer data = mesh.vertexBuffer();
		int stride = format.getVertexSize();
		long base = MemoryUtil.memAddress0(ring.mapped);
		long address = MemoryUtil.memAddress(data);
		int offset;
		if (address >= base && address + data.remaining() <= base + ring.mapped.capacity()) {
			// written in place by a ring-backed builder: nothing to copy
			offset = (int) (address - base);
		} else {
			offset = reserve(ring, stride, data.remaining());
			if (offset < 0) {
				return false;
			}
			MemoryUtil.memCopy(address, base + offset, data.remaining());
		}
		// vanilla rebinds its own VAO only when it thinks another one is current
		BufferUploader.invalidate();
		GlStateManager._glBindVertexArray(ring.vao);
		RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(state.mode());
		indices.bind(state.indexCount());
		if (shader != null) {
			shader.setDefaultUniforms(state.mode(), RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
			shader.apply();
		}
		GL32C.glDrawElementsBaseVertex(state.mode().asGLMode, state.indexCount(), indices.type().asGLType, 0, offset / stride);
		if (shader != null) {
			shader.clear();
		}
		mesh.close();
		return true;
	}

	// memory for a vertex builder inside this frame's segment, so its draw needs no copy; 0 = use normal memory
	public long reserveBuilder(VertexFormat format, int bytes) {
		if (!ZakoOptConfig.immediateRing() || !ZakoOptConfig.ringZeroCopy() || !supported()) {
			return 0;
		}
		Ring ring = ring(format);
		int offset = reserve(ring, format.getVertexSize(), bytes);
		if (offset < 0) {
			return 0;
		}
		liveBuilders++;
		return MemoryUtil.memAddress0(ring.mapped) + offset;
	}

	public void builderReleased() {
		liveBuilders--;
	}

	private Ring ring(VertexFormat format) {
		Ring ring = RINGS.computeIfAbsent(format, f -> new Ring());
		if (ring.buffer == 0) {
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

	// once per frame right before the swap
	public void endFrame() {
		if (RINGS.isEmpty()) {
			return;
		}
		int segment = (int) (frame % SEGMENTS);
		if (FENCES[segment] != 0) {
			GL32C.glDeleteSync(FENCES[segment]);
		}
		FENCES[segment] = GL32C.glFenceSync(GL32C.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
		frame++;
		await((int) (frame % SEGMENTS));
		for (Map.Entry<VertexFormat, Ring> e : RINGS.entrySet()) {
			Ring ring = e.getValue();
			if (ring.needed > 0 && liveBuilders == 0) {
				for (int i = 0; i < SEGMENTS; i++) {
					await(i);
				}
				free(ring);
				int size = Math.min(MAX_SEGMENT, Integer.highestOneBit(ring.needed) << 1);
				allocate(ring, e.getKey(), size);
				log.info("Immediate ring for {} grown to {} KB per frame", e.getKey(), size >> 10);
				ring.needed = 0;
			}
			ring.cursor = segmentStart(ring);
		}
	}

	private void await(int segment) {
		if (FENCES[segment] != 0) {
			GL32C.glClientWaitSync(FENCES[segment], GL32C.GL_SYNC_FLUSH_COMMANDS_BIT, Long.MAX_VALUE);
			GL32C.glDeleteSync(FENCES[segment]);
			FENCES[segment] = 0;
		}
	}

	private int segmentStart(Ring ring) {
		return (int) (frame % SEGMENTS) * ring.segmentSize;
	}

	private boolean supported() {
		if (supported == null) {
			GLCapabilities caps = GL.getCapabilities();
			supported = caps.OpenGL44 || caps.GL_ARB_buffer_storage;
			log.info("Immediate ring: {}", supported ? "coherent persistent mapping" : "unsupported, no ARB_buffer_storage");
		}
		return supported;
	}

	private void allocate(Ring ring, VertexFormat format, int segmentSize) {
		ring.segmentSize = segmentSize;
		int size = segmentSize * SEGMENTS;
		BufferUploader.invalidate();
		ring.vao = GlStateManager._glGenVertexArrays();
		GlStateManager._glBindVertexArray(ring.vao);
		ring.buffer = GlStateManager._glGenBuffers();
		GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, ring.buffer);
		ARBBufferStorage.glBufferStorage(GL15C.GL_ARRAY_BUFFER, size, FLAGS);
		ring.mapped = GL30C.glMapBufferRange(GL15C.GL_ARRAY_BUFFER, 0, size, FLAGS);
		// the VAO captures the attribute pointers together with the bound array buffer
		format.setupBufferState();
		GlStateManager._glBindVertexArray(0);
	}

	private void free(Ring ring) {
		GlStateManager._glDeleteVertexArrays(ring.vao);
		GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, ring.buffer);
		GL15C.glUnmapBuffer(GL15C.GL_ARRAY_BUFFER);
		GlStateManager._glDeleteBuffers(ring.buffer);
	}
}
