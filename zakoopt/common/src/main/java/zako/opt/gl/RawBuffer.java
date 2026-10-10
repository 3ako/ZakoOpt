package zako.opt.gl;

import com.mojang.blaze3d.vertex.VertexFormat;

public interface RawBuffer {
	boolean zakoopt$accepts(VertexFormat format);

	void zakoopt$push(long src, int count);
}
