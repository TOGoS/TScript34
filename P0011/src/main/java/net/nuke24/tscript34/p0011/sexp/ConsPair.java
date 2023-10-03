package net.nuke24.tscript34.p0011.sexp;

import net.nuke24.tscript34.p0011.sloc.AbstractHasSourceLocation;

public class ConsPair extends AbstractHasSourceLocation {
	public final Object left;
	public final Object right;
	
	public ConsPair(Object left, Object right, String sourceFileUri, int li, int ci, int eli, int eci) {
		super(sourceFileUri, li, ci, eli, eci);
		this.left = left;
		this.right = right;
	}
	public ConsPair(Object left, Object right) {
		super();
		this.left = left;
		this.right = right;
	}
	
	@Override public boolean equals(Object obj) {
		if( !(obj instanceof ConsPair) ) return false;
		ConsPair op = (ConsPair)obj;
		return left.equals(op.left) && right.equals(op.right) && abstractSourceLocationEquals(op);
	}
	
	@Override public int hashCode() {
		return (left.hashCode() << 1) | (right.hashCode() << 9) ^ abstractSourceLocationHashCode();
	}
	
	@Override public String toString() {
		return "ConsPair(" + left.toString() + ", " + right.toString() + getSlocString(", ")+")";
	}
}
