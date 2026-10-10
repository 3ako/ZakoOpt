package zako.opt.entity;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import zako.opt.ZakoOptConfig;
import zako.opt.gl.RawVertices;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// far entities: model vertices are written once per tick and copied, shifted to the current position, on the frames in between
@UtilityClass
public class AnimFreeze {
	public long frame;
	private final ByteBufferBuilder SCRATCH = new ByteBufferBuilder(16384);
	private ByteBuffer emitBuffer = MemoryUtil.memAlloc(16384);

	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	public final class Holder {
		List<Capture> captures = new ArrayList<>(4);
	}

	@FieldDefaults(level = AccessLevel.PRIVATE)
	final class Capture {
		Object model;
		RenderType type;
		TextureAtlasSprite sprite;
		int ordinal;
		long usedFrame = -1;
		long tick = Long.MIN_VALUE;
		ByteBuffer data;
		int count;
		float x, y, z;
	}

	// false: not frozen, the caller renders as usual
	public boolean render(Entity entity, Object model, RenderType type, TextureAtlasSprite sprite, Matrix4f pose,
								 VertexConsumer target, Consumer<VertexConsumer> draw) {
		float x = pose.m30(), y = pose.m31(), z = pose.m32();
		float d = ZakoOptConfig.entityAnimDistance();
		if (x * x + y * y + z * z <= d * d || !RawVertices.accepts(target, type.format())) {
			return false;
		}
		AnimHolder owner = (AnimHolder) entity;
		Holder holder = owner.zakoopt$anim();
		if (holder == null) {
			holder = new Holder();
			owner.zakoopt$anim(holder);
		}
		Capture capture = find(holder, model, type, sprite);
		long tick = entity.level().getGameTime();
		if (capture.tick != tick) {
			BufferBuilder builder = new BufferBuilder(SCRATCH, type.mode(), type.format());
			draw.accept(sprite == null ? builder : sprite.wrap(builder));
			capture.count = 0;
			try (MeshData mesh = builder.build()) {
				if (mesh != null) {
					ByteBuffer src = mesh.vertexBuffer();
					int bytes = src.remaining();
					if (capture.data == null || capture.data.capacity() < bytes) {
						capture.data = ByteBuffer.allocateDirect(bytes * 2);
					}
					MemoryUtil.memCopy(MemoryUtil.memAddress(src), MemoryUtil.memAddress0(capture.data), bytes);
					capture.count = mesh.drawState().vertexCount();
				}
			}
			capture.tick = tick;
			capture.x = x;
			capture.y = y;
			capture.z = z;
		}
		if (capture.count > 0) {
			emit(capture, type.format(), x - capture.x, y - capture.y, z - capture.z, target);
		}
		return true;
	}

	// the same model may be submitted several times per entity; the n-th call this frame reuses the n-th capture
	private Capture find(Holder holder, Object model, RenderType type, TextureAtlasSprite sprite) {
		int ordinal = 0;
		for (Capture c : holder.captures) {
			if (c.model == model && c.type == type && c.sprite == sprite && c.usedFrame == frame) {
				ordinal++;
			}
		}
		for (Capture c : holder.captures) {
			if (c.model == model && c.type == type && c.sprite == sprite && c.ordinal == ordinal) {
				c.usedFrame = frame;
				return c;
			}
		}
		Capture c = new Capture();
		c.model = model;
		c.type = type;
		c.sprite = sprite;
		c.ordinal = ordinal;
		c.usedFrame = frame;
		holder.captures.add(c);
		return c;
	}

	private void emit(Capture capture, VertexFormat format, float dx, float dy, float dz, VertexConsumer target) {
		int stride = format.getVertexSize();
		int pos = format.getOffset(VertexFormatElement.POSITION);
		int size = capture.count * stride;
		if (emitBuffer.capacity() < size) {
			emitBuffer = MemoryUtil.memRealloc(emitBuffer, size * 2);
		}
		long dst = MemoryUtil.memAddress0(emitBuffer);
		MemoryUtil.memCopy(MemoryUtil.memAddress0(capture.data), dst, size);
		for (long v = dst + pos, end = dst + size; v < end; v += stride) {
			MemoryUtil.memPutFloat(v, MemoryUtil.memGetFloat(v) + dx);
			MemoryUtil.memPutFloat(v + 4, MemoryUtil.memGetFloat(v + 4) + dy);
			MemoryUtil.memPutFloat(v + 8, MemoryUtil.memGetFloat(v + 8) + dz);
		}
		try (MemoryStack stack = MemoryStack.stackPush()) {
			RawVertices.push(target, stack, dst, capture.count, format);
		}
	}
}
