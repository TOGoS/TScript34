package net.nuke24.tscript34.p0010.ducer;

import net.nuke24.tscript34.p0010.Function;

/**
 * The of a stream processor, including the data
 * data output by the most recent update,
 * input data that was passed on,
 * and a function to advance the state given
 * new input data.
 */
public class DucerState2<I,O>
{
	public final Function<DucerChunk<I>,DucerState2<I,O>> processor;
	public final DucerChunk<I> passed;
	public final DucerChunk<O> output;
	public DucerState2(Function<DucerChunk<I>,DucerState2<I,O>> processor, DucerChunk<I> passed, DucerChunk<O> output) {
		this.processor = processor;
		this.passed = passed;
		this.output = output;
	}
	
	public DucerState2<I,O> process(DucerChunk<I> input) {
		return this.processor.apply(input);
	}
}
