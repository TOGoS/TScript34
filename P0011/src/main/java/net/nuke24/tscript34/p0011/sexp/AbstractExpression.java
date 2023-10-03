package net.nuke24.tscript34.p0011.sexp;

import net.nuke24.tscript34.p0011.AbstractHasSourceLocation;

public abstract class AbstractExpression extends AbstractHasSourceLocation {
	public AbstractExpression(String sourceFileUri, int li, int ci, int eli, int eci) {
		super(sourceFileUri, li, ci, eli, eci);
	}
}
