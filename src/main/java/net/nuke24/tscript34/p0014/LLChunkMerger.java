package net.nuke24.tscript34.p0014;

import java.util.ArrayList;
import java.util.List;

import net.nuke24.tscript34.p0010.Function;
import net.nuke24.tscript34.p0010.ducer.DucerChunk;
import net.nuke24.tscript34.p0010.ducer.DucerState;
import net.nuke24.tscript34.p0010.ducer.InputPortState;
import net.nuke24.tscript34.p0014.LLChunks.Chunk;
import net.nuke24.tscript34.p0014.LLChunks.ContentPiece;
import net.nuke24.tscript34.p0014.LLChunks.Header;
import net.nuke24.tscript34.p0014.LLChunks.HeaderKey;
import net.nuke24.tscript34.p0014.LLChunks.HeaderValuePiece;
import net.nuke24.tscript34.p0014.util.ArrayUtil;

/**
 * Merges adjacent chunks together that can be merged together,
 * and translate HeaderKeys into Headers.
 * 
 * Useful for canonicalization or for generating
 * a simplified chunk stream.
 */
class LLChunkMerger implements Function<DucerChunk<Chunk[]>, DucerState<Chunk[],Chunk[]>> {
	protected static final Chunk[] EMPTY_CHUNK_LIST = new Chunk[0];
	protected static final InputPortState<Chunk[]> EMPTY_OPEN_INPUT = new InputPortState<Chunk[]>(false, EMPTY_CHUNK_LIST);
	protected static final InputPortState<Chunk[]> EMPTY_CLOSED_INPUT = new InputPortState<Chunk[]>(true, EMPTY_CHUNK_LIST);
	
	/**
	 * Only merges header chunks together, but leaves
	 * [nonzero-length] content chunks alone.
	 */
	public static final LLChunkMerger FULL = new LLChunkMerger(null, true);
	/**
	 * One that leaves content chunks alone; may be desirable
	 * if content might be very large.
	 */
	public static final LLChunkMerger HEADERS_ONLY = new LLChunkMerger(null, false);
	
	final Chunk previousChunk;
	final boolean mergeContentChunks;
	
	private LLChunkMerger(Chunk previousChunk, boolean mergeContentChunks) {
		this.previousChunk = previousChunk;
		this.mergeContentChunks = mergeContentChunks;
	}
	
	// Merge the chunks, or return null if not mergeable
	protected static Chunk merge( Chunk a, Chunk b ) {
		if( a instanceof HeaderKey hka && b instanceof HeaderValuePiece vcb ) {
			return new Header(hka.key(), vcb.data());
		}
		if( a instanceof Header ha && b instanceof HeaderValuePiece vcb ) {
			return new Header(ha.key(), ha.value()+vcb.data());
		}
		if( a instanceof HeaderValuePiece vca && b instanceof HeaderValuePiece vcb ) {
			return new HeaderValuePiece(vca.data() + vcb.data());
		}
		if( a instanceof ContentPiece vca && b instanceof ContentPiece vcb ) {
			return new ContentPiece(ArrayUtil.concat(vca.data(), 0, vcb.data()));
		}
		return null;
	}
	
	@Override
	public DucerState<Chunk[], Chunk[]> apply(DucerChunk<Chunk[]> input) {
		Chunk previousChunk = this.previousChunk;
		List<Chunk> merged = new ArrayList<Chunk>();
		for( Chunk c : input.payload ) {
			if( c instanceof HeaderValuePiece hvc && hvc.data().length() == 0 ) continue;
			if( c instanceof ContentPiece cc && cc.data().length == 0 ) continue;
			
			if( c instanceof HeaderKey hk ) c = new Header(hk.key(), "");
			
			if( previousChunk == null ) {
				previousChunk = c;
				continue;
			}
			
			Chunk mergedChunk = merge(previousChunk, c);
			if( mergedChunk == null ) {
				merged.add(previousChunk);
				previousChunk = c;
			} else {			
				previousChunk = mergedChunk;
			}
		}
		if( input.isEnd() ) {
			if( previousChunk != null ) {
				merged.add(previousChunk);
				previousChunk = null;
			}
		}
		return new DucerState<Chunk[],Chunk[]>(
			new LLChunkMerger(previousChunk, mergeContentChunks),
			EMPTY_CLOSED_INPUT,
			new DucerChunk<Chunk[]>(merged.toArray(new Chunk[merged.size()]), input.isEnd())
		);
	}
}
