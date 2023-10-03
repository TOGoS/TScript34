package net.nuke24.tscript34.p0011.sloc;

final class BasicSourceLocation extends AbstractHasSourceLocation {
	public BasicSourceLocation(String sourceFileUri, int sourceLineIndex, int sourceColumnIndex, int sourceEndLineIndex, int sourceEndColumnIndex) {
		super(sourceFileUri, sourceLineIndex, sourceColumnIndex, sourceEndLineIndex, sourceEndColumnIndex);
	}
	
	@Override public boolean equals(Object obj) {
		return abstractSourceLocationEquals(obj);
	}
	
	@Override public int hashCode() {
		return abstractSourceLocationHashCode();
	}
}
