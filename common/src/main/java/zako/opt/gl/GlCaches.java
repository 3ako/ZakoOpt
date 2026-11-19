package zako.opt.gl;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.experimental.UtilityClass;

import java.util.Arrays;

// OpenGL state that the vanilla wrappers do not cache but that changes far less often than it is set. Every value is
// written only by paths that were taught to report it (vanilla's wrappers and Sodium's raw binds), so a hit means the
// context already holds exactly this value
@UtilityClass
public class GlCaches {
	private final int UNITS = 32;
	private final int[] SAMPLERS = new int[UNITS];
	private final int[] TEXTURES = new int[UNITS];
	// per texture object: GL_TEXTURE_BASE_LEVEL and GL_TEXTURE_MAX_LEVEL
	private final Int2IntOpenHashMap BASE_MIP = new Int2IntOpenHashMap();
	private final Int2IntOpenHashMap MAX_MIP = new Int2IntOpenHashMap();
	// handles of buffers whose persistent mapping was created coherent: their writes reach the GPU without a flush
	private final LongOpenHashSet COHERENT = new LongOpenHashSet();
	private int vao = -1;
	private int scissorX, scissorY, scissorWidth, scissorHeight;
	private boolean scissorKnown;
	private int polygon = -1;

	static {
		Arrays.fill(SAMPLERS, -1);
	}

	// a feature was switched: nothing may be skipped until each path reports its state again
	public void reset() {
		Arrays.fill(SAMPLERS, -1);
		Arrays.fill(TEXTURES, 0);
		BASE_MIP.clear();
		MAX_MIP.clear();
		vao = -1;
		scissorKnown = false;
		polygon = -1;
	}

	public boolean sameVao(int id) {
		if (vao == id) {
			return true;
		}
		vao = id;
		return false;
	}

	public void invalidateVao() {
		vao = -1;
	}

	public boolean sameSampler(int unit, int sampler) {
		if (unit < 0 || unit >= UNITS) {
			return false;
		}
		if (SAMPLERS[unit] == sampler) {
			return true;
		}
		SAMPLERS[unit] = sampler;
		return false;
	}

	public boolean sameScissor(int x, int y, int width, int height) {
		if (scissorKnown && scissorX == x && scissorY == y && scissorWidth == width && scissorHeight == height) {
			return true;
		}
		scissorX = x;
		scissorY = y;
		scissorWidth = width;
		scissorHeight = height;
		scissorKnown = true;
		return false;
	}

	public boolean samePolygon(int mode) {
		if (polygon == mode) {
			return true;
		}
		polygon = mode;
		return false;
	}

	public void texture(int unit, int id) {
		if (unit >= 0 && unit < UNITS) {
			TEXTURES[unit] = id;
		}
	}

	public int texture(int unit) {
		return unit >= 0 && unit < UNITS ? TEXTURES[unit] : 0;
	}

	public boolean sameMip(int texture, int pname, int value) {
		// an untracked binding may be a texture bound behind the wrapper's back: never guess for it
		if (texture == 0) {
			return false;
		}
		Int2IntOpenHashMap map = pname == 33084 ? BASE_MIP : MAX_MIP;
		if (map.getOrDefault(texture, Integer.MIN_VALUE) == value) {
			return true;
		}
		map.put(texture, value);
		return false;
	}

	// the name can be handed to a new texture, which starts with the default levels
	public void forgetTexture(int id) {
		BASE_MIP.remove(id);
		MAX_MIP.remove(id);
		for (int i = 0; i < UNITS; i++) {
			if (TEXTURES[i] == id) {
				TEXTURES[i] = 0;
			}
		}
	}

	public void markCoherent(int handle) {
		COHERENT.add(handle & 0xFFFFFFFFL);
	}

	public boolean isCoherent(int handle) {
		return COHERENT.contains(handle & 0xFFFFFFFFL);
	}

	public void forgetBuffer(int handle) {
		COHERENT.remove(handle & 0xFFFFFFFFL);
	}
}
