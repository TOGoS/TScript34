package net.nuke24.jcr36;

public class QuitAction implements JCRAction {
	public final int exitCode;
	public QuitAction(int exitCode) {
		this.exitCode = exitCode;
	}
}
