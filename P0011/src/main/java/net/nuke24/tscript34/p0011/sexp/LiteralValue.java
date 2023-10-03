package net.nuke24.tscript34.p0011.sexp;

import net.nuke24.tscript34.p0011.sloc.AbstractHasSourceLocation;

public class LiteralValue extends AbstractHasSourceLocation {
	public final Object value;
	
	public LiteralValue(Object value, String sfu, int sli, int sci, int seli, int seci ) {
		super(sfu, sli, sci, seli, seci);
		this.value = value;
	}
	public LiteralValue(Object value) {
		this.value = value;
	}
	
	@Override public String toString() {
		return getClass().getSimpleName()+"("+value+getSlocString(", ")+")";
	}
}
