package net.nuke24.tscript34.p0019.effect;

import net.nuke24.tscript34.p0019.util.DebugFormat;

// Hint: What if multiple values?  Then we could have multithreading!
public class ResumeWith<T> {
	public static final ResumeWith<?> blank = new ResumeWith<Object>(null);
	
	public final T value;
	public ResumeWith(T value) {
		this.value = value;
	}
	
	@Override public String toString() {
		return "ResumeWith[" + DebugFormat.toDebugString(value) + "]";
	}
}
