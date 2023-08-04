package net.nuke24.jcr36;

// For JRE-1.6 compatibility
// Equivalent to java.util.function.Function
public interface Function<I,O> {
	public O apply(I in);
}
