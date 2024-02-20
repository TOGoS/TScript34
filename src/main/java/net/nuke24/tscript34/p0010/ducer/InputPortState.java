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
 * 
 * 2024-02-09 Note: This is a bad way to model this because
 * the case where input is NOT closed, but there is data queued,
 * is hard to handle, especially in an abstract way.
 * When updating a DucerState, one would need to be careful to always process
 * any queued data before processing new data.
 * 
 * If input data is only ever completely rejected
 * (as opposed to rejected only for now),
 * this problem goes away.  The earlier Ducer scheme
 * was in this way better than the current one.
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
