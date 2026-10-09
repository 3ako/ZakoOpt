package zako.opt.entity;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GL;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

// player skins copied (GPU to GPU) into one texture, so all players share one render type and draw call
@UtilityClass
public class SkinAtlas {
	public final Identifier ID = Identifier.fromNamespaceAndPath("zakoopt", "skin_atlas");
	private final int SIZE = 1024;
	private final int SKIN = 64;
	private final int PER_ROW = SIZE / SKIN;
	// access order: the eldest entry is the least recently drawn skin
	private final Map<Identifier, Slot> SLOTS = new LinkedHashMap<>(64, 0.75f, true);
	private final ArrayDeque<Integer> FREE = new ArrayDeque<>();
	private Texture texture;
	private NativeImage placeholder;
	private long frame;
	private Boolean supported;

	private final class Slot {
		final int index;
		final TextureAtlasSprite sprite;
		GpuTexture source;
		long lastFrame;

		Slot(int index, TextureAtlasSprite sprite) {
			this.index = index;
			this.sprite = sprite;
		}
	}

	private final class Texture extends AbstractTexture {
		Texture() {
			GpuDevice device = RenderSystem.getDevice();
			texture = device.createTexture(() -> "zakoopt skin atlas", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.RGBA8, SIZE, SIZE, 1, 1);
			textureView = device.createTextureView(texture);
			sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
			device.createCommandEncoder().clearColorTexture(texture, 0);
		}
	}

	private final class Sprite extends TextureAtlasSprite {
		Sprite(SpriteContents contents, int x, int y) {
			super(ID, contents, SIZE, SIZE, x, y, 0);
		}
	}

	public void endFrame() {
		frame++;
	}

	// null = draw this skin the vanilla way (not loaded yet, odd size, atlas full of skins drawn this frame)
	public TextureAtlasSprite sprite(Identifier skin) {
		if (supported == null) {
			supported = GL.getCapabilities().OpenGL43 || GL.getCapabilities().GL_ARB_copy_image;
		}
		if (!supported) {
			return null;
		}
		if (texture == null) {
			texture = new Texture();
			Minecraft.getInstance().getTextureManager().register(ID, texture);
			placeholder = new NativeImage(1, 1, false);
			for (int i = 0; i < PER_ROW * PER_ROW; i++) {
				FREE.add(i);
			}
		}
		GpuTexture source = Minecraft.getInstance().getTextureManager().getTexture(skin).getTexture();
		if (source.getWidth(0) != SKIN || source.getHeight(0) != SKIN || source.isClosed() || !(source instanceof GlTexture src)) {
			return null;
		}
		Slot slot = SLOTS.get(skin);
		if (slot == null) {
			slot = allocate(skin);
			if (slot == null) {
				return null;
			}
		}
		if (slot.source != source) {
			int x = slot.index % PER_ROW * SKIN, y = slot.index / PER_ROW * SKIN;
			// raw texture copy: blaze3d's copy is a framebuffer blit, which the current colour write mask can spoil
			GL43C.glCopyImageSubData(src.glId(), GL11C.GL_TEXTURE_2D, 0, 0, 0, 0,
					((GlTexture) texture.getTexture()).glId(), GL11C.GL_TEXTURE_2D, 0, x, y, 0, SKIN, SKIN, 1);
			slot.source = source;
		}
		slot.lastFrame = frame;
		return slot.sprite;
	}

	private Slot allocate(Identifier skin) {
		Integer index = FREE.poll();
		if (index == null) {
			Iterator<Map.Entry<Identifier, Slot>> eldest = SLOTS.entrySet().iterator();
			Slot victim = eldest.next().getValue();
			if (victim.lastFrame == frame) {
				// every slot is in use this frame; overwriting one would change a skin already submitted
				return null;
			}
			eldest.remove();
			index = victim.index;
		}
		int x = index % PER_ROW * SKIN, y = index / PER_ROW * SKIN;
		Slot slot = new Slot(index, new Sprite(new SpriteContents(skin, new FrameSize(SKIN, SKIN), placeholder), x, y));
		SLOTS.put(skin, slot);
		return slot;
	}
}
