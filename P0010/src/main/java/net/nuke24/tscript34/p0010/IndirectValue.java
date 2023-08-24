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
	 * Returns a representation of this IndirectValue (NOT of the object it represents!).
	 * 
	 * e.g. an indirect value with tag = 5 (REFERENCE) should be able to as(Supplier and/or JTSupplier)
	 * so that you can get() the reference.  Whether the IndirectValue instance /is/ or /has/
	 * the [JT]Supplier is left to the IndirectValue implementation.
	 */
	public <T> T as(Class<T> repClass);
}
