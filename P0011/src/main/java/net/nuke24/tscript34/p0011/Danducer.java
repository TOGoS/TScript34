package net.nuke24.tscript34.p0011;

public interface Danducer<I,O> {
	public static class DucerData<I,O> {
		public final Danducer<I,O> state;
		/** Provides a way to pass unprocessed input back to the previous processor */
		public final I remainingInput;
		public final O output;
		public final boolean isDone;
		public DucerData(Danducer<I,O> state, I remainingInput, O output, boolean isDone) {
			this.state = state;
			this.remainingInput = remainingInput;
			this.output = output;
			this.isDone = isDone;
		}
		public DucerData<I,O> process(I input, boolean endOfInput) {
			return this.state.process(input, endOfInput);
		}
	}
	
	public DucerData<I,O> process(I input, boolean endOfInput);
}
