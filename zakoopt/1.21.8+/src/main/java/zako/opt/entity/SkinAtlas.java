package zako.opt.entity;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTexture;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GL;
import zako.opt.MixinPlugin;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

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
	private AtlasTexture texture;
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

	public void endFrame() {
		frame++;
	}

	// null = draw this skin the vanilla way (not loaded yet, odd size, atlas full of skins drawn this frame)
	public TextureAtlasSprite sprite(ResourceLocation skin) {
		GpuTexture source = Minecraft.getInstance().getTextureManager().getTexture(skin).getTexture();
		// GL only: the copy is a raw GL call, and with the Vulkan backend there is no GL context to ask
		if (!(source instanceof GlTexture src) || source.getWidth(0) != SKIN || source.getHeight(0) != SKIN || source.isClosed()) {
			return null;
		}
		if (supported == null) {
			// VulkanMod keeps the GL texture classes but leaves no GL context on the render thread: asking GL for its capabilities throws
			supported = !MixinPlugin.VULKAN && (GL.getCapabilities().OpenGL43 || GL.getCapabilities().GL_ARB_copy_image);
		}
		if (!supported) {
			return null;
		}
		if (texture == null) {
			texture = new AtlasTexture("zakoopt skin atlas", SIZE);
			Minecraft.getInstance().getTextureManager().register(ID, texture);
			placeholder = new NativeImage(1, 1, false);
			for (int i = 0; i < PER_ROW * PER_ROW; i++) {
				FREE.add(i);
			}
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
		Slot slot = new Slot(index, new AtlasSprite(ID, AtlasSprite.contents(skin, SKIN, placeholder), SIZE, x, y));
		SLOTS.put(skin, slot);
		return slot;
	}
}
