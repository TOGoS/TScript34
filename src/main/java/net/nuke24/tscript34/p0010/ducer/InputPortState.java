package net.nuke24.tscript34.p0010.ducer;

/**
 * Represents the state of a Danducer input port,
 * which can be open or closed, and have some
 * queued but not-processed data.
 * 
 * If closed, the queued data will *never* be processed
 * by the Danducer in question.
 * 
 * Counterpart to a DucerChunk,
 * which can be thought to represent the state of an output port.
 */
public class InputPortState<I> {
	/**
	 * Whether the port is closed.
	 * Once closed, anything left in the queue can be safely
	 * discarded or passed to a different port.
	 */
	public final boolean isClosed;
	public final I queued;
	
	public InputPortState(boolean isClosed, I queued) {
		this.isClosed = isClosed;
		this.queued = queued;
	}
	public boolean isClosed() {
		return isClosed;
	}
}
