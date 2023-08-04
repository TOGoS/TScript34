package net.nuke24.jcr36.action;

public class Quit implements JCRAction {
	public final int exitCode;
	public Quit(int exitCode) {
		this.exitCode = exitCode;
	}
	@Override public String toString() {
		return getClass().getSimpleName()+"("+exitCode+")";
	}
}
