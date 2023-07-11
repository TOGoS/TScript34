package net.nuke24.jcr36;

public class QuitAction implements JCRAction {
	public final int exitCode;
	public QuitAction(int exitCode) {
		this.exitCode = exitCode;
	}
	@Override public String toString() {
		return getClass().getSimpleName()+"("+exitCode+")";
	}
}
