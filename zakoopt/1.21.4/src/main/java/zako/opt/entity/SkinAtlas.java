package zako.opt.entity;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL43C;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

// player skins copied (GPU to GPU) into one texture, so all players share one render type and draw call
@UtilityClass
public class SkinAtlas {
	public final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("zakoopt", "skin_atlas");
	private final int SIZE = 1024;
	private final int SKIN = 64;
	private final int PER_ROW = SIZE / SKIN;
	// access order: the eldest entry is the least recently drawn skin
	private final Map<ResourceLocation, Slot> SLOTS = new LinkedHashMap<>(64, 0.75f, true);
	private final ArrayDeque<Integer> FREE = new ArrayDeque<>();
	// 1.21.4 textures do not know their size; asked from GL once per texture object
	private final Map<AbstractTexture, Boolean> SKIN_SIZED = new WeakHashMap<>();
	private Texture texture;
	private NativeImage placeholder;
	private long frame;
	private Boolean supported;

	private final class Slot {
		final int index;
		final TextureAtlasSprite sprite;
		AbstractTexture source;
		long lastFrame;

		Slot(int index, TextureAtlasSprite sprite) {
			this.index = index;
			this.sprite = sprite;
		}
	}

	private final class Texture extends AbstractTexture {
		Texture() {
			TextureUtil.prepareImage(getId(), 0, SIZE, SIZE);
			setFilter(false, false);
			try (NativeImage blank = new NativeImage(SIZE, SIZE, true)) {
				blank.upload(0, 0, 0, false);
			}
		}
	}

	private final class Sprite extends TextureAtlasSprite {
		Sprite(SpriteContents contents, int x, int y) {
			super(ID, contents, SIZE, SIZE, x, y);
		}
	}

	public void endFrame() {
		frame++;
	}

	// null = draw this skin the vanilla way (not loaded yet, odd size, atlas full of skins drawn this frame)
	public TextureAtlasSprite sprite(ResourceLocation skin) {
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
		AbstractTexture source = Minecraft.getInstance().getTextureManager().getTexture(skin);
		if (!SKIN_SIZED.computeIfAbsent(source, SkinAtlas::skinSized)) {
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
			GL43C.glCopyImageSubData(source.getId(), GL11C.GL_TEXTURE_2D, 0, 0, 0, 0,
					texture.getId(), GL11C.GL_TEXTURE_2D, 0, x, y, 0, SKIN, SKIN, 1);
			slot.source = source;
		}
		slot.lastFrame = frame;
		return slot.sprite;
	}

	private boolean skinSized(AbstractTexture source) {
		GlStateManager._bindTexture(source.getId());
		return GL11C.glGetTexLevelParameteri(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_TEXTURE_WIDTH) == SKIN
				&& GL11C.glGetTexLevelParameteri(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_TEXTURE_HEIGHT) == SKIN;
	}

	private Slot allocate(ResourceLocation skin) {
		Integer index = FREE.poll();
		if (index == null) {
			Iterator<Map.Entry<ResourceLocation, Slot>> eldest = SLOTS.entrySet().iterator();
			Slot victim = eldest.next().getValue();
			if (victim.lastFrame == frame) {
				// every slot is in use this frame; overwriting one would change a skin already submitted
				return null;
			}
			eldest.remove();
			index = victim.index;
		}
		int x = index % PER_ROW * SKIN, y = index / PER_ROW * SKIN;
		Slot slot = new Slot(index, new Sprite(new SpriteContents(skin, new FrameSize(SKIN, SKIN), placeholder, ResourceMetadata.EMPTY), x, y));
		SLOTS.put(skin, slot);
		return slot;
	}
}
