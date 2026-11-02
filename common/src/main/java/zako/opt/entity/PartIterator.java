package zako.opt.entity;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.model.geom.ModelPart;

import java.util.Iterator;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
public final class PartIterator implements Iterator<ModelPart> {
	public final ModelPart[] parts;
	public int next;

	@Override
	public boolean hasNext() {
		return next < parts.length;
	}

	@Override
	public ModelPart next() {
		if (next >= parts.length) {
			throw new NoSuchElementException();
		}
		return parts[next++];
	}
}
