package net.nuke24.jcr36;

// Does nothing.
public final class NullAction implements JCRAction {
	public static final NullAction INSTANCE = new NullAction();
	
	private NullAction() { }
}
