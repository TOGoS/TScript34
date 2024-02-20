package net.nuke24.tscript34.p0014.util;

import net.nuke24.tscript34.p0010.Function;
import net.nuke24.tscript34.p0010.ducer.DucerChunk;
import net.nuke24.tscript34.p0010.ducer.DucerState;

public class DucerUtil {
	private static class ChainDucer<T0,T1,T2> implements Function<DucerChunk<T0>, DucerState<T0,T2>> {
		final DucerState<T0,T1> state1;
		final DucerState<T1,T2> state2;
		public ChainDucer(
			DucerState<T0,T1> state1,
			DucerState<T1,T2> state2
		) {
			this.state1 = state1;
			this.state2 = state2;
		}
		
		@Override
		public DucerState<T0, T2> apply(DucerChunk<T0> input) {
			DucerState<T0,T1> state1 = this.state1.processor.apply(input);
			DucerState<T1,T2> state2 = this.state2.processor.apply(state1.output);
			
			// TROUBLE IN DANDUCER LAND:
			// It would probably be best to state that
			// the returned input port of a danducer
			// should only be non-empty when it is closed.
			// Otherwise callers need to track unconsumed inputs
			// which is inconvenient!
			// 
			// The older design, where the state was either open
			// or closed, was maybe better, since the only time
			// you had to care about unconsumed input was
			// when everything was finished.
			
			return new DucerState<T0, T2>(
				new ChainDucer<T0,T1,T2>(state1, state2),
				state1.input, state2.output);
		}
		
		public DucerState<T0, T2> toDucerState() {
			return new DucerState<T0, T2>(
				this,
				state1.input, state2.output
			);
		}
	}
	
	public static <T0,T1,T2> DucerState<T0,T2> chain(
		DucerState<T0,T1> state1,
		DucerState<T1,T2> state2
	) {
		return new ChainDucer<T0,T1,T2>(state1, state2).toDucerState();
	}
}
