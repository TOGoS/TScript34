package net.nuke24.tscript34.p0011.sexp;

public class Atom extends AbstractExpression {
	public final String text;
	
	public Atom(String text, String sourceFileUri, int li, int ci, int eli, int eci) {
		super(sourceFileUri, li, ci, eli, eci);
		this.text = text;
	}
}
