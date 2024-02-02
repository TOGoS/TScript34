package net.nuke24.tscript34.p0010;

/**
 * Represents a value by an encoding identified by the tag.
 * Inspired by P0005's TT interface, but using ints
 * instead of Strings for 'efficiency' (to avoid if-else chains
 * in JDK 1.6 and earlier, which doesn't allow switching on Strings).
 */
public interface IndirectValue {
	/**
	 * Returns an integer corresponding to one of the IndirectValueTags.* constants,
	 * indicating how this object should be interpreted.
	 */
	public int getTag();
	/**
	 * Converts this IndirectValue to some other representation.
	 * What classes are recognized and how they represent the concept
	 * is left to the implementation, but there should be conventions
	 * for common types:
	 * 
	 * - for references, as(URIReference.class).get() returns the name of the referenced concept
	 * - for lists/cons pairs, as(Pair.class) returns a pair representing the first
	 *   node of a linked list.
	 * 
	 * While IndirectValue itself is simultaneously precise and abstract,
	 * the meaning of objects returned by as depends on implementation.
	 * e.g. a system that uses IndirectValues may use one to store a rational
	 * number, where as(Long.class) returns a 64-bit number that unconventionally
	 * encodes both the numerator and denominator; i.e. not the usual
	 * meaning of a `Long`!  It's probably good practice to avoid overriding
	 * classes that have an 'obvious' alternate meaning, and to represent
	 * such encodings using the IndirectValue system itself;
	 * i.e. using a tag that specifically means 'rational number represented
	 * as an integer using funny encoding X', and then as(Long.class) or
	 * as(Integer.class) would return that encoding.
	 */
	public <T> T as(Class<T> repClass);
}
