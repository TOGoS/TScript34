package net.nuke24.tscript34.p0014;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;
import net.nuke24.tscript34.p0010.Function;
import net.nuke24.tscript34.p0010.ducer.DucerChunk;
import net.nuke24.tscript34.p0010.ducer.DucerState;
import net.nuke24.tscript34.p0010.ducer.InputPortState;
import net.nuke24.tscript34.p0014.LLChunks.Chunk;
import net.nuke24.tscript34.p0014.LLChunks.ContentPiece;
import net.nuke24.tscript34.p0014.LLChunks.Header;
import net.nuke24.tscript34.p0014.LLChunks.HeaderKey;
import net.nuke24.tscript34.p0014.LLChunks.HeaderValuePiece;
import net.nuke24.tscript34.p0014.LLChunks.NewEntryLine;
import net.nuke24.tscript34.p0014.util.ArrayUtil;

public class TEFParserTest extends TestCase
{
	static final Charset UTF8 = Charset.forName("UTF-8");
	
	static class LLChunkMerger implements Function<DucerChunk<Chunk[]>, DucerState<Chunk[],Chunk[]>> {
		protected static final Chunk[] EMPTY_CHUNK_LIST = new Chunk[0];
		protected static final InputPortState<Chunk[]> EMPTY_OPEN_INPUT = new InputPortState<Chunk[]>(false, EMPTY_CHUNK_LIST);
		protected static final InputPortState<Chunk[]> EMPTY_CLOSED_INPUT = new InputPortState<Chunk[]>(true, EMPTY_CHUNK_LIST);
		public static final LLChunkMerger INIT = new LLChunkMerger(null);
		
		final Chunk previousChunk;
		
		private LLChunkMerger(Chunk previousChunk) {
			this.previousChunk = previousChunk;
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
				new LLChunkMerger(previousChunk),
				EMPTY_CLOSED_INPUT,
				new DucerChunk<Chunk[]>(merged.toArray(new Chunk[merged.size()]), input.isEnd())
			);
		}
	}
	
	protected static List<Chunk> normalize(List<Chunk> chunks) {
		DucerState<Chunk[],Chunk[]> ds = LLChunkMerger.INIT.apply(new DucerChunk<Chunk[]>(chunks.toArray(new Chunk[chunks.size()]), true));
		assert ds.isDone();
		assert ds.input.queued.length == 0;
		return Arrays.asList(ds.output.payload);
	}
	static List<Chunk> normalize(Chunk[] chunks) {
		return normalize(Arrays.asList(chunks));
	}
	
	static byte[][] randomlyChunk(byte[] input, Random r) {
		List<byte[]> chunked = new ArrayList<>();
		int i=0;
		if( r.nextBoolean() ) {
			// Sometimes include an empty chunk at the beginning
			chunked.add(Arrays.copyOfRange(input, i, i));
		}
		while( i<input.length ) {
			int chunkSize = (r.nextInt() & 0xFFFF) % input.length;
			chunkSize = Math.min(chunkSize, input.length-i);
			assert chunkSize >= 0;
			chunked.add(Arrays.copyOfRange(input, i, i+chunkSize));
			i += chunkSize;
		}
		if( r.nextBoolean() ) {
			// Sometimes include an empty chunk at the end
			chunked.add(Arrays.copyOfRange(input, i, i));
		}
		return chunked.toArray(new byte[chunked.size()][]);
	}
	
	void testParsesAs(
		Chunk[] expected,
		String source
	) {
		byte[] sourceBytes = source.getBytes(UTF8);
		for( int s=0; s<20; ++s ) {
			byte[][] chunks = randomlyChunk(sourceBytes, new Random(s));
			DucerState<byte[],Chunk[]> parseState = TEFParser.INIT.apply(new DucerChunk<byte[]>(new byte[0], false));
			List<Chunk> output = new ArrayList<Chunk>();
			for( int i=0; i<chunks.length; ++i ) {
				parseState = parseState.process(new DucerChunk<byte[]>(chunks[i], i == chunks.length-1));
				for(Chunk c : parseState.output.payload) {
					output.add(c);
				}
			}
			assertEquals(normalize(expected), normalize(output));
		}
	}
	
	public void testParseNewEntryLine() {
		testParsesAs(
			new Chunk[] { new NewEntryLine("hi", "Hello, world!") },
			"=hi Hello, world!"
		);
	}
	public void testParseNewEntryLineAndLf() {
		testParsesAs(
			new Chunk[] { new NewEntryLine("hi", "Hello, world!") },
			"=hi Hello, world!\n"
		);
	}
	public void testParseNewEntryAndBlankLines() {
		testParsesAs(
			new Chunk[] { new NewEntryLine("hi", "Hello, world!") },
			"=hi Hello, world!\n\n"
		);
	}
	public void testParseHeader() {
		testParsesAs(
			new Chunk[] { new Header("hi there", "foo bar") },
			"hi there: foo bar"
		);
	}
	public void testParseNewEntryAndHeader() {
		testParsesAs(
			new Chunk[] {
				new NewEntryLine("foo", "bar baz"),
				new Header("hi there", "foo bar")
			},
			"=foo bar baz\n"+
			"hi there: foo bar"
		);
	}
	public void testParseNewEntryAndMultilineHeader() {
		testParsesAs(
			new Chunk[] {
				new NewEntryLine("foo", "bar baz"),
				new Header("hi there", "foo bar\nbaz quux"),
			},
			"=foo bar baz\n"+
			"hi there: foo bar\n"+
			"\tbaz quux"
		);
	}
	public void testLotsaDifferentHaders() {
		testParsesAs(
			new Chunk[] {
				new NewEntryLine("foo", "bar baz"),
				new Header("hi:there", "foo bar\nbaz quux"),
				new Header("bill", "ted"),
				new Header("kyanu", "\nhorsey"),
				new Header("kevin", "\n"),
			},
			"=foo bar baz\n"+
			"hi:there: foo bar\n"+
			"\tbaz quux\n"+
			"bill: ted\n"+
			"kyanu:\n"+
			"\thorsey\n"+
			"kevin: \n"+
			"\t"
		);
	}
	public void testParseEntryWithContent() {
		testParsesAs(
			new Chunk[] {
				new NewEntryLine("foo", "bar"),
				new ContentPiece("baz".getBytes(UTF8))
			},
			"=foo bar\n"+
			"\n"+
			"baz"
		);
	}
	public void testParseEntryWithBareKey() {
		testParsesAs(
			new Chunk[] { new Header("foo", "") },
			"foo:"
		);
	}
	
	public static void main(String[] args) {
		new TEFParserTest().testParseEntryWithContent();
	}
}
