package net.nuke24.tscript34.p0019.util;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathUtil {
	// Copied from JavaCommandRunner36's SimplerCommandRunner
	static final Pattern WIN_PATH_MATCHER = Pattern.compile("^([a-z]):(.*)", Pattern.CASE_INSENSITIVE);
	/** @param unc set to true to prefer UNC (//host/...) paths */
	public static String resolveFilePath(File pwd, String path, boolean unc) {
		path = path.replace("\\", "/");
		boolean relativeResolved = false;
		Matcher m;
		while( true ) {
			if( path.startsWith("//") ) {
				return path;
			} else if( path.startsWith("/") ) {
				return unc ? "//" + path : path;
			} else if( (m = WIN_PATH_MATCHER.matcher(path)).matches() ) {
				String winPath = m.group(1).toUpperCase()+":"+m.group(2);
				return unc ? "///" + winPath : winPath;
			} else if( !relativeResolved ) {
				// relative
				path = new File(pwd, path).getAbsolutePath().replace("\\","/");
				relativeResolved = true;
			} else {
				// Warning might be in order here
				return path;
			}
		}
	}
}
