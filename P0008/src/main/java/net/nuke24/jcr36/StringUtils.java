package net.nuke24.jcr36;

public class StringUtils {
	private StringUtils() { }
	
	public static boolean parseBool(String str, String context) {
		str = str.toLowerCase().trim();
		if( "false".equals(str) || "no".equals(str) || "n".equals(str) || "off".equals(str) || "0".equals(str) ) {
			return false;
		} else if( "true".equals(str) || "yes".equals(str) || "y".equals(str) || "on".equals(str) || "1".equals(str) ) {
			return true;
		} else {
			throw new IllegalArgumentException("Don't know how to interpret '"+str+"' as boolean in '"+context+"'");
		}
	}
	
	// Quote in the conventional C/Java/JSON style.
	// Don't rely on this for passing to other programs!
	public static String quote(String s) {
		return
			"\"" +
			s.replace("\\","\\\\")
			 .replace("\"", "\\\"")
			 .replace("\r","\\r")
			 .replace("\n","\\n")
			 .replace("\t","\\t")
			 .replace(""+(char)0x1b,"\\x1B")
			+"\"";
	}
}
