package net.nuke24.tscript34.p0019.effect;

import net.nuke24.tscript34.p0019.util.DebugFormat;

/** Exit from a loop, or a program, or whatever. */
public class ReturnWithValue<T> {
	public final T value;
	public ReturnWithValue(T value) {
		this.value = value;
	}
	
	@Override public String toString() {
		return "ReturnWithValue["+DebugFormat.toDebugString(value)+"]";
	}
}
