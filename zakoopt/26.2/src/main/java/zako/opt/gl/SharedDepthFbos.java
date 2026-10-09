package zako.opt.gl;

import com.mojang.blaze3d.opengl.GlStateManager;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import lombok.experimental.UtilityClass;
import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11C;

// a depth texture -> a cached FBO that already has it attached at mip 0, so clearing it needs no attach/detach
@UtilityClass
public class SharedDepthFbos {
	private final Int2IntOpenHashMap FBO_BY_DEPTH = new Int2IntOpenHashMap();
	private final float[] VALUE = new float[1];
	private Boolean dsa;

	public void remember(int depthId, int fbo) {
		FBO_BY_DEPTH.put(depthId, fbo);
	}

	public void forget(int fbo) {
		FBO_BY_DEPTH.values().removeIf(f -> f == fbo);
	}

	// false = do it the vanilla way
	public boolean clear(int depthId, double depth) {
		if (dsa == null) {
			dsa = GL.getCapabilities().OpenGL45 || GL.getCapabilities().GL_ARB_direct_state_access;
		}
		int fbo = FBO_BY_DEPTH.getOrDefault(depthId, 0);
		if (!dsa || fbo == 0) {
			return false;
		}
		// glClearNamedFramebuffer obeys the depth write mask and scissor test, like glClear in the vanilla path
		GlStateManager._depthMask(true);
		GlStateManager._disableScissorTest();
		VALUE[0] = (float) depth;
		ARBDirectStateAccess.glClearNamedFramebufferfv(fbo, GL11C.GL_DEPTH, 0, VALUE);
		return true;
	}
}
