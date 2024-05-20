package net.nuke24.tscript34.p0019;

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
}
