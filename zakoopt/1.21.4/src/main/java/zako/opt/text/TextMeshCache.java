package zako.opt.text;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import zako.opt.ZakoOptConfig;
import zako.opt.gl.RawVertices;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

// 1.21.4 lays text out glyph by glyph on every draw. A line seen again is drawn once in its own space into a recorder,
// keeping the vanilla order of buffer switches, and from then on replayed: positions through the caller's matrix, light
// swapped in. A line is recorded on its second sighting, so text that changes every frame stays on the vanilla path.
@UtilityClass
public class TextMeshCache {
	private final Matrix4f IDENTITY = new Matrix4f();
	private final ByteBufferBuilder SCRATCH = new ByteBufferBuilder(4096);
	private final Mesh SEEN = new Mesh(List.of(), 0);
	private final Mesh UNCACHEABLE = new Mesh(List.of(), 0);
	private final Map<Key, Mesh> CACHE = new LinkedHashMap<>(512, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Key, Mesh> eldest) {
			return size() > 4096;
		}
	};
	private boolean recording;

	private record Segment(RenderType type, ByteBuffer vertices, int count) {
	}

	private record Mesh(List<Segment> segments, int width) {
	}

	public record Key(FormattedCharSequence text, float x, float y, int color, boolean shadow, Font.DisplayMode mode, int background, boolean inverseDepth) {
		@Override
		public boolean equals(Object o) {
			return o instanceof Key k && k.text == text && k.x == x && k.y == y && k.color == color && k.shadow == shadow
					&& k.mode == mode && k.background == background && k.inverseDepth == inverseDepth;
		}

		@Override
		public int hashCode() {
			int h = System.identityHashCode(text);
			h = 31 * h + Float.floatToIntBits(x);
			h = 31 * h + Float.floatToIntBits(y);
			h = 31 * h + color;
			h = 31 * h + background;
			h = 31 * h + mode.ordinal();
			return 31 * h + (shadow ? 1 : 0) + (inverseDepth ? 2 : 0);
		}
	}

	public boolean applies(FormattedCharSequence text) {
		return !recording && ZakoOptConfig.preparedTextCache() && StableText.isStable(text) && RenderSystem.isOnRenderThread();
	}

	public Matrix4f identity() {
		return IDENTITY;
	}

	// null: draw the vanilla way; record draws the line into the given buffers with the identity matrix and light 0
	public Integer draw(Key key, Matrix4f pose, MultiBufferSource buffers, int light, ToIntFunction<MultiBufferSource> record) {
		Mesh mesh = CACHE.get(key);
		if (mesh == null) {
			CACHE.put(key, SEEN);
			return null;
		}
		if (mesh == UNCACHEABLE) {
			return null;
		}
		if (mesh == SEEN) {
			// obfuscated glyphs are re-randomized on every draw, so they must not be frozen
			if (!key.text.accept((index, style, codePoint) -> !style.isObfuscated())) {
				CACHE.put(key, UNCACHEABLE);
				return null;
			}
			mesh = record(record);
			CACHE.put(key, mesh);
		}
		return emit(mesh, pose, buffers, light) ? mesh.width : null;
	}

	public void clear() {
		CACHE.clear();
	}

	private Mesh record(ToIntFunction<MultiBufferSource> draw) {
		Recorder recorder = new Recorder();
		recording = true;
		int width;
		try {
			width = draw.applyAsInt(recorder);
		} finally {
			recording = false;
			recorder.flush();
		}
		return new Mesh(recorder.segments, width);
	}

	private boolean emit(Mesh mesh, Matrix4f pose, MultiBufferSource buffers, int light) {
		Vector3f tmp = new Vector3f();
		for (int s = 0; s < mesh.segments.size(); s++) {
			Segment segment = mesh.segments.get(s);
			VertexFormat format = segment.type.format();
			VertexConsumer target = buffers.getBuffer(segment.type);
			// ponytail: only the first buffer is checked, a text's buffers all come from one source
			if (s == 0 && !RawVertices.accepts(target, format)) {
				return false;
			}
			int stride = format.getVertexSize();
			int pos = format.getOffset(VertexFormatElement.POSITION);
			int uv2 = format.contains(VertexFormatElement.UV2) ? format.getOffset(VertexFormatElement.UV2) : -1;
			int size = segment.count * stride;
			try (MemoryStack stack = MemoryStack.stackPush()) {
				long dst = stack.nmalloc(16, size);
				MemoryUtil.memCopy(MemoryUtil.memAddress(segment.vertices), dst, size);
				for (long v = dst, end = dst + size; v < end; v += stride) {
					pose.transformPosition(MemoryUtil.memGetFloat(v + pos), MemoryUtil.memGetFloat(v + pos + 4), MemoryUtil.memGetFloat(v + pos + 8), tmp);
					MemoryUtil.memPutFloat(v + pos, tmp.x);
					MemoryUtil.memPutFloat(v + pos + 4, tmp.y);
					MemoryUtil.memPutFloat(v + pos + 8, tmp.z);
					if (uv2 >= 0) {
						MemoryUtil.memPutInt(v + uv2, light);
					}
				}
				RawVertices.push(target, stack, dst, segment.count, format);
			}
		}
		return true;
	}

	// one segment per run of draws into the same render type, as the vanilla buffer source would see them
	private static final class Recorder implements MultiBufferSource {
		final List<Segment> segments = new ArrayList<>();
		RenderType type;
		BufferBuilder builder;

		@Override
		public VertexConsumer getBuffer(RenderType renderType) {
			if (renderType != type) {
				flush();
				type = renderType;
				builder = new BufferBuilder(SCRATCH, renderType.mode(), renderType.format());
			}
			return builder;
		}

		void flush() {
			if (builder == null) {
				return;
			}
			try (MeshData mesh = builder.build()) {
				if (mesh != null) {
					ByteBuffer src = mesh.vertexBuffer();
					ByteBuffer copy = ByteBuffer.allocateDirect(src.remaining());
					MemoryUtil.memCopy(MemoryUtil.memAddress(src), MemoryUtil.memAddress(copy), src.remaining());
					segments.add(new Segment(type, copy, mesh.drawState().vertexCount()));
				}
			}
			builder = null;
			type = null;
		}
	}
}
