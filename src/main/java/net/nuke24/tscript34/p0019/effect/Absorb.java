package net.nuke24.tscript34.p0019.effect;

import net.nuke24.tscript34.p0019.util.DebugFormat;

/** Inverse of Emit.  Read an item from input. */
public class Absorb<C> {
	public final C channel;
	public Absorb(C channel) {
		this.channel = channel;
	}
	
	@Override public String toString() {
		return "Absorb[channel="+DebugFormat.toDebugString(channel)+"]";
	}
}
