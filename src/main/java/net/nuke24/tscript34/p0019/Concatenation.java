package net.nuke24.tscript34.p0019;

/**
 * Abstract representation of a sequence made by concatenating
 * several a number of sub-sequences.
 * 
 * Doesn't actually know the type of its elements,
 * or how sub-sequences are represented!
 * 
 * A concatenation with children.length = 0 is a valid
 * concatenation of any element type.
 * */
public class Concatenation<T> {
	final Object[] children; 
	public Concatenation(Object[] children) {
		this.children = children;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		String sep = "";
		sb.append("Concatenation[");
		for( Object child : children ) {
			sb.append(sep);
			sb.append(child.toString());
		}
		sb.append("]");
		return sb.toString();
	}
}
