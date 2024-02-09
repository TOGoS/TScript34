package net.nuke24.tscript34.p0010.ducer;

import net.nuke24.tscript34.p0010.Function;

/**
 * The state of a stream processor, including
 * queued-byt-not-yet-consumed input data, and output data.
 * Input and output may be independently closed.
 * Input may be closed before all data is consumed.
 */
public class DucerState<I,O> {
	public final Function<DucerChunk<I>,DucerState<I,O>> processor;
	public final InputPortState<I> input;
	public final DucerChunk<O> output;
	public DucerState(Function<DucerChunk<I>,DucerState<I,O>> processor, InputPortState<I> input, DucerChunk<O> output) {
		// It would not make sense to close input without closing output,
		// since it would be stuck in an apparently-alive-but-unable-to-progress
		// (since any progress depends on inputs) state!
		if( input.isClosed ) assert output.isEnd;
		this.processor = processor;
		this.input = input;
		this.output = output;
	}
	
	//// For use by callers
	
	public DucerState<I,O> process(DucerChunk<I> input) {
		return this.processor.apply(input);
	}
	
	/**
	 * If true, that means that this Danducer has finished
	 * doing all the work it will ever do.
	 * It will neither consume input nor generate output,
	 * so feel free to stop simulating it.
	 */
	public boolean isDone() {
		return this.input.isClosed && this.output.isEnd;
	}
	
	//// For use when constructing DucerStates ////
	
	/**
	 * For use by the internal processor.
	 * To indicate end of input data, call process() with chunk that isEnd.
	 * 
	 * Return a new state identical to this one but whose input
	 * port is closed.
	 */
	public DucerState<I,O> closeInput() {
		return this.input.isClosed ? this : new DucerState<I,O>(processor, new InputPortState<I>(true, this.input.queued), output);
	}
	/**
	 * For use by internal processor.
	 * Return a new state identical to this one but whose
	 * input port is closed.
	 */
	public DucerState<I,O> closeOutput() {
		return this.output.isEnd ? this : new DucerState<I,O>(processor, input, new DucerChunk<O>(this.output.payload, true));
	}
	/**
	 * For use by internal processor.
	 * Return a new state identical to this one but whose
	 * input and output ports are closed, which implies
	 * that this Danducer 'isDone'
	 */
	public DucerState<I,O> close() {
		return this.closeOutput().closeInput();
	}
}
