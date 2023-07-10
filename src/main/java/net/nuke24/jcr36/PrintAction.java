package net.nuke24.jcr36;

import java.util.Objects;

public class PrintAction implements JCRAction {
	public final String text;
	public final int fd;
	public PrintAction(String text, int fd) {
		this.text = text;
		this.fd = fd;
	}
	
	@Override public boolean equals(Object other) {
		if( !(other instanceof PrintAction) ) return false;
		
		PrintAction opa = (PrintAction)other;
		return this.text.equals(opa.text) && this.fd == opa.fd;
	}
	
	@Override public int hashCode() {
		return Objects.hash("PrintAction", this.text, this.fd);
	}
	
	@Override public String toString() {
		return "PrintAction("+StringUtils.quote(text)+", "+fd+")";
	}
}
