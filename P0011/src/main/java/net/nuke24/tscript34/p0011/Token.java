package net.nuke24.tscript34.p0011;

public class Token extends AbstractHasSourceLocation {
	public final String text;
	public final int mode;
	
	public Token(
		String text, int mode,
		String sourceFileUri,
		int sourceLineIndex, int sourceColumnIndex,
		int sourceEndLineIndex, int sourceEndColumnIndex
	) {
		super(sourceFileUri, sourceLineIndex, sourceColumnIndex, sourceEndLineIndex, sourceEndColumnIndex);
		this.text = text;
		this.mode = mode;
	}
	
	public Token(String text, int mode) {
		this(text, mode, null, -1, -1, -1, -1);
	}
	
	@Override public boolean equals(Object obj) {
		if( !(obj instanceof Token) ) return false;
		Token ot = (Token)obj;
		return text.equals(ot.text) && mode == ot.mode &&
			(this.sourceFileUri == ot.sourceFileUri ||
			(this.sourceFileUri != null && this.sourceFileUri.equals(ot.sourceFileUri))) &&
			abstractSourceLocationEquals(ot);
	}
	
	@Override public int hashCode() {
		return text.hashCode() ^ mode +
			(this.sourceFileUri == null ? 0 : this.sourceFileUri.hashCode()) +
			abstractSourceLocationHashCode();
	}
	
	@Override public String toString() {
		String slocStr = getSlocString();
		return "Token(\"" + text + "\", "+mode+(slocStr.isEmpty() ? "" : ", "+slocStr)+")";
	}
}
