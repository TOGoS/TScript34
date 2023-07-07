package net.nuke24.jcr36;

import java.util.Map;

public class LetEnv implements JCRAction {
	public final Map<String,String> bindings;
	public final JCRAction action;
	
	public LetEnv(Map<String,String> bindings, JCRAction action) {
		this.bindings = bindings;
		this.action = action;
	}
	
	public static JCRAction of(Map<String,String> bindings, JCRAction action) {
		return bindings.size() == 0 ? action : new LetEnv(bindings, action);
	}
}
