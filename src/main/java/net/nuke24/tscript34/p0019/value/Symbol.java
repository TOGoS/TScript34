package net.nuke24.tscript34.p0019.value;

import net.nuke24.tscript34.p0019.util.DebugFormat;

/**
 * Symbolic representation of a value.
 * 
 * Some common symbols and the concept or Java value that they represent:
 * 
 * http://www.w3.org/1999/02/22-rdf-syntax-ns#nil = empty list
 * http://ns.nuke24.net/TOGVM/Constants/True      = Boolean.TRUE
 * http://ns.nuke24.net/TOGVM/Constants/False     = Boolean.FALSE
 **/
public class Symbol {
	public final String name;
	public Symbol(String name) {
		this.name = name;
	}
	
	@Override public String toString() {
		return "Symbol[" + DebugFormat.toDebugString(this.name) + "]";
	}
	
	@Override public boolean equals(Object o) {
		if( o instanceof Symbol ) {
			Symbol os = (Symbol)o;
			return name.equals(os.name);
		}
		return false;
	}
	
	@Override public int hashCode() {
		return this.getClass().hashCode() ^ name.hashCode();
	}
}
