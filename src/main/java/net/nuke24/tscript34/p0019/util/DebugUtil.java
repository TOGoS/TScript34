package net.nuke24.tscript34.p0019.util;

public class DebugUtil {
	public static <T> T todo(String message) {
		throw new RuntimeException("TODO: "+message);
	}
	public static void debug(String message) {
		System.err.println("#DEBUG "+message);
	}
}
