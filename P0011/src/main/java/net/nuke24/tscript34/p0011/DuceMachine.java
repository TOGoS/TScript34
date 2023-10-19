package net.nuke24.tscript34.p0011;

import net.nuke24.tscript34.p0011.Danducer.DucerData;

/** 
 * Similar to Danducer but allows any number of inputs/outputs,
 * or other 'environment', which can be updated along with the machine.
 * The machine can modify its inputs and outputs at will.
 * 
 * I am not sure how useful this is, given that it is super abstract and generic.
 * But maybe that's actually great.
 */
public interface DuceMachine<E> {
	// Adapter classes for Danducers:
	class DanducerQueue<S> {
		public final S content;
		public final boolean isDone;
		public DanducerQueue(S content, boolean isDone) {
			this.content = content;
			this.isDone = isDone;
		}
		public DanducerQueue<S> with(S content, boolean isDone) {
			if( content == this.content && isDone == this.isDone ) return this;
			return new DanducerQueue<S>(content, isDone);
		}
		public DanducerQueue<S> withContent(S content) {
			if( content == this.content ) return this;
			return new DanducerQueue<S>(content, isDone);
		}
	}
	class DanducerEnv<I,O> {
		public final DanducerQueue<I> input;
		public final DanducerQueue<O> output;
		public DanducerEnv(DanducerQueue<I> input, DanducerQueue<O> output) {
			this.input = input;
			this.output = output;
		}
		public DanducerEnv<I,O> with(DanducerQueue<I> input, DanducerQueue<O> output) {
			if( input == this.input && output == this.output ) return this;
			return new DanducerEnv<I,O>(input, output);
		}
	}
	class DanducerMachine<IS,OS> implements DuceMachine<DanducerEnv<IS,OS>> {
		final Danducer<IS,OS> danducer;
		
		public DanducerMachine(Danducer<IS,OS> danducer) {
			this.danducer = danducer;
		}
		
		@Override
		public DuceState<DanducerEnv<IS, OS>> apply(DuceState<DanducerEnv<IS, OS>> duceState) {
			assert(duceState.machineState == this);
			DucerData<IS,OS> data = danducer.process(duceState.environmentState.input.content, duceState.environmentState.input.isDone);
			boolean outputUnchanged =
				data.output == duceState.environmentState.output.content &&
				data.isDone == duceState.environmentState.output.isDone;
			boolean inputUnchanged = (data.remainingInput == duceState.environmentState.input.content); 
			if(
				data.state == this.danducer &&
				outputUnchanged &&
				inputUnchanged
			) {
				// Then nothing has changed!
				return duceState;
			}
			return new DuceState<DanducerEnv<IS,OS>>(
				new DanducerMachine<IS,OS>(data.state),
				duceState.environmentState.with(
					duceState.environmentState.input.withContent(data.remainingInput),
					duceState.environmentState.output.with(data.output, data.isDone)
				)
			);
		}
	}
	
	public static class DuceState<E> {
		public final DuceMachine<E> machineState;
		public final E environmentState;
		
		public DuceState( DuceMachine<E> machineState, E envState ) {
			this.machineState = machineState;
			this.environmentState = envState;
		}
		
		public DuceState<E> withEnvironmentState(E envState) {
			return new DuceState<E>(machineState, envState);
		}
		
		public DuceState<E> update() {
			return this.machineState.apply(this);
		}
	}
	
	/**
	 * queueState.machineState should == this.
	 * Passing in the queueState allows it to return the same queueState if
	 * nothing is to be done.
	 */
	DuceState<E> apply(DuceState<E> duceState);
}
