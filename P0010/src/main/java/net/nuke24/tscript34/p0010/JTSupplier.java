package net.nuke24.tscript34.p0010;

/**
 * J[ava] T[yped] [Value] Supplier:
 * A Supplier with the potential to dynamically return different
 * representations of the value based on the requested Java class.
 */
public interface JTSupplier {
	public <V> V get(Class<V> expectedType);
}
