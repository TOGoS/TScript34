package net.nuke24.tscript34.p0019.effect;

import java.io.IOException;

public class ParseException extends IOException {
	public final String filename;
	public final int lineNumber;
	public ParseException(String message, String filename, int lineNumber) {
		super(message);
		this.filename = filename;
		this.lineNumber = lineNumber;
	}
}
