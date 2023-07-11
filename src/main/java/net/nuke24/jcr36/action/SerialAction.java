package net.nuke24.jcr36.action;

import java.util.Arrays;
import java.util.List;

public class SerialAction implements JCRAction {
	public final List<JCRAction> children;
	public SerialAction(List<JCRAction> children) {
		this.children = children;
	}
	public SerialAction(JCRAction...children) {
		this(Arrays.asList(children));
	}
}
