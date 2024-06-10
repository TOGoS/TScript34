package net.nuke24.tscript34.p0019.iface;

/**
 * Procedure<I,C,E,O> like a function<I,O>, but takes a context:C and can throw an exception:E.
 * 
 * In Unison terms, roughly: I ->{C,E} O.
 * 
 * 'function running in context' has semantic connotations different than
 * a BiFunction, namely that the context is something that may be modified
 * by the procedure, wheras regular arguments can usually be thought of
 * as immutable.
 */
public interface Procedure<I,C,E extends Throwable,O> {
	public O apply(I input, C context) throws E;
}

