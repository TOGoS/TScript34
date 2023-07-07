package net.nuke24.jcr36;

import java.util.Objects;

public class PrintAction implements JCRAction {
	public final String text;
	public PrintAction(String text) { this.text = text; }
	
	@Override public boolean equals(Object other) {
		if( !(other instanceof PrintAction) ) return false;
		
		PrintAction opa = (PrintAction)other;
		return this.text.equals(opa.text);
	}
	
	@Override public int hashCode() {
		return Objects.hash("PrintAction", this.text);
	}
}
