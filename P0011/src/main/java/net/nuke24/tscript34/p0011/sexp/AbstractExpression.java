package net.nuke24.tscript34.p0011.sexp;

import net.nuke24.tscript34.p0011.AbstractHasSourceLocation;
import net.nuke24.tscript34.p0011.HasSourceLocation;

public abstract class AbstractExpression extends AbstractHasSourceLocation {
	public AbstractExpression() { }
	public AbstractExpression(HasSourceLocation sLoc) {
		super(sLoc);
	}
	public AbstractExpression(String sourceFileUri, int li, int ci, int eli, int eci) {
		super(sourceFileUri, li, ci, eli, eci);
	}
	
	@Override public boolean equals(Object obj) {
		throw new RuntimeException("TODO: Implement "+getClass().getName()+"#equals");
	}
	@Override public int hashCode() {
		throw new RuntimeException("TODO: Implement "+getClass().getName()+"#hashCode");
	}
	@Override public String toString() {
		return getClass().getName()+"(TODO: implement toString())";
	}
}
