package net.nuke24.tscript34.p0019.effect;

import net.nuke24.tscript34.p0019.util.DebugFormat;

public class Emit<C,T> {
	public final C channel;
	public final T value;
	public Emit(C channel,T value) {
		this.channel = channel;
		this.value = value;
	}
	
	@Override public String toString() {
		return "Emit[channel="+DebugFormat.toDebugString(channel)+", value="+DebugFormat.toDebugString(value)+"]";
	}
}
