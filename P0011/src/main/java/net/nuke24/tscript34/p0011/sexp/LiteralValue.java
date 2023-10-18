package net.nuke24.tscript34.p0011.sexp;

import net.nuke24.tscript34.p0011.sloc.AbstractHasSourceLocation;
import net.nuke24.tscript34.p0011.sloc.HasSourceLocation;

public class LiteralValue extends AbstractHasSourceLocation {
	public final Object value;
	
	public LiteralValue(Object value, String sfu, int sli, int sci, int seli, int seci ) {
		super(sfu, sli, sci, seli, seci);
		this.value = value;
	}
	public LiteralValue(Object value, HasSourceLocation sLoc ) {
		super(sLoc);
		this.value = value;
	}
	public LiteralValue(Object value) {
		this.value = value;
	}
	
	@Override public boolean equals(Object obj) {
		if( !(obj instanceof LiteralValue) ) return false;
		LiteralValue a = (LiteralValue)obj;
		return value.equals(a.value) && abstractSourceLocationEquals(obj);
	}
	@Override public int hashCode() {
		return 505 + this.value.hashCode() + abstractSourceLocationHashCode() << 7;
	}
	
	@Override public String toString() {
		return getClass().getSimpleName()+"("+value+getSlocString(", ")+")";
	}
}
