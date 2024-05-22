package net.nuke24.tscript34.p0019.util;

public class DebugFormat {
	private DebugFormat() { }
	
	public static String toDebugString(Object o) {
		if( o == null ) {
			return "null";
		} else if( o instanceof CharSequence ) {
			return "\"" + o.toString().replace("\"", "\\\"").replace("\\", "\\\\") + "\"";
		} else {
			return o.toString();
		}
	}
}
