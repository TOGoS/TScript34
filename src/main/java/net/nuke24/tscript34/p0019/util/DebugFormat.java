package net.nuke24.tscript34.p0019.util;

import net.nuke24.tscript34.p0019.value.Symbol;

public class DebugFormat {
	private DebugFormat() { }
	
	public static String toDebugString(Object o) {
		if( o == null ) {
			return "null";
		} else if( o instanceof CharSequence ) {
			return "\"" + o.toString().replace("\"", "\\\"").replace("\\", "\\\\") + "\"";
		} else if( o instanceof Symbol ) {
			return "<"+((Symbol)o).name+">";
		} else {
			return o.toString();
		}
	}
}
