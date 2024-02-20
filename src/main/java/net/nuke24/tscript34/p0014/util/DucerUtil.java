package net.nuke24.tscript34.p0014.util;

import net.nuke24.tscript34.p0010.Function;
import net.nuke24.tscript34.p0010.ducer.DucerChunk;
import net.nuke24.tscript34.p0010.ducer.DucerState2;

public class DucerUtil {
	private static class ChainDucer<T0,T1,T2> implements Function<DucerChunk<T0>, DucerState2<T0,T2>> {
		final DucerState2<T0,T1> state1;
		final DucerState2<T1,T2> state2;
		public ChainDucer(
			DucerState2<T0,T1> state1,
			DucerState2<T1,T2> state2
		) {
			this.state1 = state1;
			this.state2 = state2;
		}
		
		@Override
		public DucerState2<T0, T2> apply(DucerChunk<T0> input) {
			DucerState2<T0,T1> state1 = this.state1.processor.apply(input);
			DucerState2<T1,T2> state2 = this.state2.processor.apply(state1.output);
			
			return new DucerState2<T0, T2>(
				new ChainDucer<T0,T1,T2>(state1, state2),
				// Note that any state2.passed is ignored
				state1.passed, state2.output);
		}
		
		public DucerState2<T0, T2> toDucerState() {
			return new DucerState2<T0, T2>(this, state1.passed, state2.output);
		}
	}
	
	public static <T0,T1,T2> DucerState2<T0,T2> chain(
		DucerState2<T0,T1> state1,
		DucerState2<T1,T2> state2
	) {
		return new ChainDucer<T0,T1,T2>(state1, state2).toDucerState();
	}
}
