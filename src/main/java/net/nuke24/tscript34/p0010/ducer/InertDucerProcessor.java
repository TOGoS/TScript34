package net.nuke24.tscript34.p0010.ducer;

import net.nuke24.tscript34.p0010.Function;

/**
 * A ducer processor that will only ever
 * pass on its input and output empty chunks.
 */
class InertDucerProcessor<I,O> implements Function<DucerChunk<I>,DucerState2<I,O>> {
	final DucerChunk<O> repeatForever;
	/** @param repeatForever presumably empty (to make this really 'inert') output chunk */
	public InertDucerProcessor(DucerChunk<O> repeatForever) {
		this.repeatForever = repeatForever;
	}
	
	@Override
	public DucerState2<I, O> apply(DucerChunk<I> input) {
		return new DucerState2<I,O>(this, input, repeatForever);
	}
}
