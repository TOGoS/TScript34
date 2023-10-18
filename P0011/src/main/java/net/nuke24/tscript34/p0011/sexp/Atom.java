package net.nuke24.tscript34.p0011.sexp;

import net.nuke24.tscript34.p0011.sloc.AbstractHasSourceLocation;
import net.nuke24.tscript34.p0011.sloc.HasSourceLocation;

public class Atom extends AbstractHasSourceLocation {
	public final String text;
	
	public Atom(String text, String sourceFileUri, int li, int ci, int eli, int eci) {
		super(sourceFileUri, li, ci, eli, eci);
		this.text = text;
	}
	public Atom(String text, HasSourceLocation sLoc) {
		super(sLoc);
		this.text = text;
	}
	public Atom(String text) {
		this.text = text;
	}
	
	@Override public boolean equals(Object obj) {
		if( !(obj instanceof Atom) ) return false;
		Atom a = (Atom)obj;
		return text.equals(a.text) && abstractSourceLocationEquals(obj);
	}
	@Override public int hashCode() {
		return 303 + this.text.hashCode() + abstractSourceLocationHashCode() << 7;
	}
	
	@Override public String toString() {
		return "Atom(\""+this.text+getSlocString(", ")+"\")";
	}
}