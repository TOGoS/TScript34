package net.nuke24.tscript34.p0011.sexp;

public class ConsPair extends AbstractExpression {
	public final AbstractExpression left;
	public final AbstractExpression right;
	
	public ConsPair(AbstractExpression left, AbstractExpression right, String sourceFileUri, int li, int ci, int eli, int eci) {
		super(sourceFileUri, li, ci, eli, eci);
		this.left = left;
		this.right = right;
	}
}
