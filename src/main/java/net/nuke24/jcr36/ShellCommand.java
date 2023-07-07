package net.nuke24.jcr36;

import java.util.function.Function;

public class ShellCommand implements JCRAction {
	public final String[] argv;
	public final Function<Integer, JCRAction> onExit;
	
	public ShellCommand(String[] argv, Function<Integer, JCRAction> onExit) {
		this.argv = argv;
		this.onExit = onExit;
	}
}
