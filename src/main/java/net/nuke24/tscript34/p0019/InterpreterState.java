package net.nuke24.tscript34.p0019;

public interface InterpreterState<A,E> {
	/**
	 * Return an object indicating an effect that the interpreted program
	 * wants to perform, and whose result (if any) should be passed to advance().
	 * May be null to indicate that nothing is requested
	 * (e.g. if the interpreter has stopped due to having done maxSteps)
	 */
	public E getRequest();
	/**
	 * Provide the given value (which may be null in some situations)
	 * in response to the request.
	 * Advance the state of the interpreter by up to 'maxSteps' steps
	 * (the meaning of a 'step' is arbitrary).
	 * Returns the new state of the interpreter.
	 */
	InterpreterState<? super A,? extends E> advance(A arg, int maxSteps);
}
