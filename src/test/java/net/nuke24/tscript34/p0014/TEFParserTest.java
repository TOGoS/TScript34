package net.nuke24.tscript34.p0014;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;
import net.nuke24.tscript34.p0010.ducer.DucerChunk;
import net.nuke24.tscript34.p0010.ducer.DucerState;
import net.nuke24.tscript34.p0014.LLChunks.Chunk;
import net.nuke24.tscript34.p0014.LLChunks.ContentPiece;
import net.nuke24.tscript34.p0014.LLChunks.Header;
import net.nuke24.tscript34.p0014.LLChunks.NewEntryLine;

public class TEFParserTest extends TestCase
{
	static final Charset UTF8 = Charset.forName("UTF-8");
	
	protected static List<Chunk> normalize(List<Chunk> chunks) {
		DucerState<Chunk[],Chunk[]> ds = LLChunkMerger.FULL.apply(new DucerChunk<Chunk[]>(chunks.toArray(new Chunk[chunks.size()]), true));
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
