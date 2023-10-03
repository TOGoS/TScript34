package net.nuke24.tscript34.p0011;

public class Token implements HasSourceLocation {
	public final String text;
	public final int mode;
	public final String sourceFileUri;
	public final int sourceLineIndex;
	public final int sourceColumnIndex;
	public final int sourceEndLineIndex;
	public final int sourceEndColumnIndex;
	
	public Token(
		String text, int mode,
		String sourceFilename,
		int sourceLineIndex, int sourceColumnIndex,
		int sourceEndLineIndex, int sourceEndColumnIndex
	) {
		this.text = text;
		this.mode = mode;
		this.sourceFileUri = sourceFilename;
		this.sourceLineIndex = sourceLineIndex;
		this.sourceColumnIndex = sourceColumnIndex;
		this.sourceEndLineIndex = sourceEndLineIndex;
		this.sourceEndColumnIndex = sourceEndColumnIndex;
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
			this.sourceLineIndex == ot.sourceLineIndex &&
			this.sourceColumnIndex == ot.sourceColumnIndex &&
			this.sourceEndLineIndex == ot.sourceEndLineIndex &&
			this.sourceEndColumnIndex == ot.sourceEndColumnIndex;
	}
	
	@Override public int hashCode() {
		return text.hashCode() ^ mode +
			(this.sourceFileUri == null ? 0 : this.sourceFileUri.hashCode()) +
			(this.sourceLineIndex      <<  0) +
			(this.sourceColumnIndex    <<  8) +
			(this.sourceEndLineIndex   << 16) +
			(this.sourceEndColumnIndex << 24);
	}
	
	@Override public String toString() {
		String slocStr = (
			this.sourceFileUri == null &&
			sourceLineIndex == -1 && sourceColumnIndex == -1 &&
			sourceEndLineIndex == -1 && sourceEndColumnIndex == -1
		) ? "" : ", \""+
			sourceFileUri+"#"+
			(sourceLineIndex+1)+","+(sourceColumnIndex+1)+".."+
			(sourceEndLineIndex+1)+","+(sourceEndColumnIndex+1)+"\"";
		return "Token(\"" + text + "\", "+mode+slocStr+")";
	}
	
	@Override public String getSourceFileUri() { return sourceFileUri; }
	@Override public int getSourceLineIndex() { return sourceLineIndex; }
	@Override public int getSourceColumnIndex() { return sourceColumnIndex; }
	@Override public int getSourceEndLineIndex() { return sourceEndLineIndex; }
	@Override public int getSourceEndColumnIndex() { return sourceEndColumnIndex; }
}