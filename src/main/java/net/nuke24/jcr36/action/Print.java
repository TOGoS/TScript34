package net.nuke24.jcr36.action;

import java.util.Objects;

import net.nuke24.jcr36.StringUtils;

public class Print implements JCRAction {
	public final String text;
	public final int fd;
	public Print(String text, int fd) {
		this.text = text;
		this.fd = fd;
	}
	
	@Override public boolean equals(Object other) {
		if( !(other instanceof Print) ) return false;
		
		Print opa = (Print)other;
		return this.text.equals(opa.text) && this.fd == opa.fd;
	}
	
	@Override public int hashCode() {
		return Objects.hash(getClass().getSimpleName(), this.text, this.fd);
	}
	
	@Override public String toString() {
		return getClass().getSimpleName()+"("+StringUtils.quote(text)+", "+fd+")";
	}
}
