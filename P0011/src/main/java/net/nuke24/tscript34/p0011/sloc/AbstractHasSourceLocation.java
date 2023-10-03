package net.nuke24.tscript34.p0011.sloc;

public class AbstractHasSourceLocation implements HasSourceLocation {
	public final String sourceFileUri;
	public final int sourceLineIndex;
	public final int sourceColumnIndex;
	public final int sourceEndLineIndex;
	public final int sourceEndColumnIndex;	
	
	public AbstractHasSourceLocation(
		String sourceFileUri,
		int sourceLineIndex, int sourceColumnIndex,
		int sourceEndLineIndex, int sourceEndColumnIndex
	) {
		this.sourceFileUri = sourceFileUri;
		this.sourceLineIndex = sourceLineIndex;
		this.sourceColumnIndex = sourceColumnIndex;
		this.sourceEndLineIndex = sourceEndLineIndex;
		this.sourceEndColumnIndex = sourceEndColumnIndex;
	}
	
	public AbstractHasSourceLocation(HasSourceLocation sLoc) {
		this(
			sLoc.getSourceFileUri(),
			sLoc.getSourceLineIndex(), sLoc.getSourceColumnIndex(),
			sLoc.getSourceEndLineIndex(), sLoc.getSourceEndColumnIndex()
		);
	}
	
	public AbstractHasSourceLocation() {
		this(null, -1, -1, -1, -1);
	}
	
	protected boolean abstractSourceLocationEquals(Object obj) {
		if( !(obj instanceof HasSourceLocation) ) return false;
		HasSourceLocation hsl = (HasSourceLocation)obj;
		return
			(this.sourceFileUri == hsl.getSourceFileUri() ||
			 (this.sourceFileUri != null && this.sourceFileUri.equals(hsl.getSourceFileUri()))) &&
			this.sourceLineIndex == hsl.getSourceLineIndex() &&
			this.sourceColumnIndex == hsl.getSourceColumnIndex() &&
			this.sourceEndLineIndex == hsl.getSourceEndLineIndex() &&
			this.sourceEndColumnIndex == hsl.getSourceEndColumnIndex();
	}
	
	protected int abstractSourceLocationHashCode() {
		return
			(this.sourceFileUri == null ? 0 : this.sourceFileUri.hashCode()) +
			(this.sourceLineIndex      <<  0) +
			(this.sourceColumnIndex    <<  8) +
			(this.sourceEndLineIndex   << 16) +
			(this.sourceEndColumnIndex << 24);
	}
	
	protected String getSlocString(String prefix) {
		return (
			this.sourceFileUri == null &&
			sourceLineIndex == -1 && sourceColumnIndex == -1 &&
			sourceEndLineIndex == -1 && sourceEndColumnIndex == -1
		) ? "" : prefix + "<"+
			sourceFileUri+">#"+
			(sourceLineIndex+1)+","+(sourceColumnIndex+1)+".."+
			(sourceEndLineIndex+1)+","+(sourceEndColumnIndex+1);
	}
	
	@Override public String toString() {
		return getClass().getSimpleName()+"("+getSlocString("")+")";
	}
	
	@Override public String getSourceFileUri() { return sourceFileUri; }
	@Override public int getSourceLineIndex() { return sourceLineIndex; }
	@Override public int getSourceColumnIndex() { return sourceColumnIndex; }
	@Override public int getSourceEndLineIndex() { return sourceEndLineIndex; }
	@Override public int getSourceEndColumnIndex() { return sourceEndColumnIndex; }
}
