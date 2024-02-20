package net.nuke24.tscript34.p0014;

import java.util.ArrayList;
import java.util.List;

import net.nuke24.tscript34.p0010.Function;
import net.nuke24.tscript34.p0010.ducer.DucerChunk;
import net.nuke24.tscript34.p0010.ducer.DucerState2;
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
class LLChunkMerger implements Function<DucerChunk<Chunk[]>, DucerState2<Chunk[],Chunk[]>> {
	protected static final Chunk[] EMPTY_CHUNK_LIST = new Chunk[0];
	protected static final DucerChunk<Chunk[]> EMPTY_OPEN_OUTPUT = new DucerChunk<Chunk[]>(EMPTY_CHUNK_LIST, false);
	protected static final DucerChunk<Chunk[]> EMPTY_OPEN_INPUT = new DucerChunk<Chunk[]>(EMPTY_CHUNK_LIST, false);
	protected static final DucerChunk<Chunk[]> EMPTY_CLOSED_INPUT = new DucerChunk<Chunk[]>(EMPTY_CHUNK_LIST, true);
	
	/**
	 * Only merges header chunks together, but leaves
	 * [nonzero-length] content chunks alone.
	 */
	public static final DucerState2<Chunk[],Chunk[]> FULL = new DucerState2<Chunk[],Chunk[]>(
		new LLChunkMerger(null, true), EMPTY_OPEN_INPUT, EMPTY_OPEN_OUTPUT);
	/**
	 * One that leaves content chunks alone; may be desirable
	 * if content might be very large.
	 */
	public static final DucerState2<Chunk[],Chunk[]> HEADERS_ONLY = new DucerState2<Chunk[],Chunk[]>(
		new LLChunkMerger(null, false), EMPTY_OPEN_INPUT, EMPTY_OPEN_OUTPUT);
	
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
	public DucerState2<Chunk[], Chunk[]> apply(DucerChunk<Chunk[]> input) {
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
		return new DucerState2<Chunk[],Chunk[]>(
			new LLChunkMerger(previousChunk, mergeContentChunks),
			input.isEnd() ? EMPTY_CLOSED_INPUT : EMPTY_OPEN_INPUT,
			new DucerChunk<Chunk[]>(merged.toArray(new Chunk[merged.size()]), input.isEnd())
		);
	}
}
