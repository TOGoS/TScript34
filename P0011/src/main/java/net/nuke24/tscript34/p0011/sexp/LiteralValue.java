package net.nuke24.tscript34.p0011.sexp;

public class LiteralValue extends AbstractExpression {
	public final Object value;
	
	public LiteralValue(Object value, String sfu, int sli, int sci, int seli, int seci ) {
		super(sfu, sli, sci, seli, seci);
		this.value = value;
	}
	public LiteralValue(Object value) {
		this.value = value;
	}
}
