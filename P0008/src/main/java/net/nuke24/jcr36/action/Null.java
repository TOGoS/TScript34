package net.nuke24.jcr36.action;

// Does nothing.
public final class Null implements JCRAction {
	public static final Null INSTANCE = new Null();
	
	private Null() { }
}
