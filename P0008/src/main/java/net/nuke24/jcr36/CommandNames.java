package net.nuke24.jcr36;

import java.util.LinkedHashMap;

public class CommandNames {
	public static final String CMD_DOCMD = "http://ns.nuke24.net/JavaCommandRunner36/Action/DoCmd";
	public static final String CMD_PRINT = "http://ns.nuke24.net/JavaCommandRunner36/Action/Print";
	
	public static final LinkedHashMap<String,String> DEFAULT_ALIASES = new LinkedHashMap<String,String>();
	static {
		DEFAULT_ALIASES.put("jcr:docmd", CMD_DOCMD);
		DEFAULT_ALIASES.put("jcr:print", CMD_PRINT);
	}
}
