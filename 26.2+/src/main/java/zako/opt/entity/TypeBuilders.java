package zako.opt.entity;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.rendertype.RenderType;

@UtilityClass
public class TypeBuilders {
	public BufferBuilder create(ByteBufferBuilder buffer, RenderType type) {
		return new BufferBuilder(buffer, type.primitiveTopology(), type.format());
	}

	public int positionOffset(VertexFormat format) {
		return format.getElement(DefaultVertexFormat.POSITION_SEMANTIC_NAME).offset();
	}
}
