package remodik.opt.entity;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;

// a square sprite placed by hand in an atlas
public class AtlasSprite extends TextureAtlasSprite {
	public AtlasSprite(ResourceLocation atlas, SpriteContents contents, int atlasSize, int x, int y) {
		super(atlas, contents, atlasSize, atlasSize, x, y);
	}

	// contents of a size x size sprite whose pixels never come from the image: the atlas is filled on the GPU
	public static SpriteContents contents(ResourceLocation id, int size, NativeImage image) {
		return new SpriteContents(id, new FrameSize(size, size), image);
	}
}
