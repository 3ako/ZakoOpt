package zako.opt.gl;

public interface ReserveBuffer extends RawBuffer {
	// claims room for count vertices filled in later; the result is an offset, as the memory may move when the buffer grows
	long zakoopt$reserve(int count);

	long zakoopt$address(long offset);
}
