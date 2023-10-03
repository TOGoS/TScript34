package net.nuke24.tscript34.p0011.sloc;

public interface HasSourceLocation {
	public String getSourceFileUri();
	public int getSourceLineIndex();
	public int getSourceColumnIndex();
	public int getSourceEndLineIndex();
	public int getSourceEndColumnIndex();
}
