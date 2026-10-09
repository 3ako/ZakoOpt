package zako.opt.gl;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.experimental.UtilityClass;
import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL;

// depth textures already attached to some colour texture's FBO, so a depth clear needs no bind/attach/detach round trip
@UtilityClass
public class DepthFbos {
	private record Owner(GlTexture color, int fbo) {
	}

	private final Int2ObjectOpenHashMap<Owner> OWNERS = new Int2ObjectOpenHashMap<>();
	private final float[] VALUE = new float[1];
	private Boolean dsa;

	public void remember(GlTexture color, int depthId, int fbo) {
		OWNERS.put(depthId, new Owner(color, fbo));
	}

	// false = do it the vanilla way
	public boolean clear(int depthId, double depth) {
		if (dsa == null) {
			dsa = GL.getCapabilities().OpenGL45 || GL.getCapabilities().GL_ARB_direct_state_access;
		}
		Owner owner = OWNERS.get(depthId);
		if (!dsa || owner == null || owner.color.isClosed()) {
			return false;
		}
		// glClearNamedFramebuffer obeys the depth write mask and scissor test, like glClear in the vanilla path
		GlStateManager._depthMask(true);
		GlStateManager._disableScissorTest();
		VALUE[0] = (float) depth;
		ARBDirectStateAccess.glClearNamedFramebufferfv(owner.fbo, GL11C.GL_DEPTH, 0, VALUE);
		return true;
	}
}
