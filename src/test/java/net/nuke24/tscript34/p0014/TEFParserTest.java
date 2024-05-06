package net.nuke24.tscript34.p0014;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;
import net.nuke24.tscript34.p0010.ducer.DucerChunk;
import net.nuke24.tscript34.p0010.ducer.DucerState2;
import net.nuke24.tscript34.p0014.LLChunks.Chunk;
import net.nuke24.tscript34.p0014.LLChunks.ContentPiece;
import net.nuke24.tscript34.p0014.LLChunks.Header;
import net.nuke24.tscript34.p0014.LLChunks.NewEntryLine;
import net.nuke24.tscript34.p0014.util.DucerUtil;

public class TEFParserTest extends TestCase
{
	static final Charset UTF8 = Charset.forName("UTF-8");
	
	protected static List<Chunk> normalize(List<Chunk> chunks) {
		DucerState2<Chunk[],Chunk[]> ds = LLChunkMerger.FULL.process(new DucerChunk<Chunk[]>(chunks.toArray(new Chunk[chunks.size()]), true));
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
		List<Chunk> normExpected = normalize(expected);
		
		byte[] sourceBytes = source.getBytes(UTF8);
		for( int s=0; s<20; ++s ) {
			byte[][] chunks = randomlyChunk(sourceBytes, new Random(s));
			DucerState2<byte[],Chunk[]> parseState = DucerUtil.chain(
				TEFParser.INIT,
				LLChunkMerger.FULL
			);
			List<Chunk> output = new ArrayList<Chunk>();
			for( int i=0; i<chunks.length; ++i ) {
				parseState = parseState.process(new DucerChunk<byte[]>(chunks[i], i == chunks.length-1));
				for(Chunk c : parseState.output.payload) {
					output.add(c);
				}
			}
			assertEquals(normExpected, normalize(output));
		}
	}
	
	public void testParseNewEntryLine() {
		testParsesAs(
			new Chunk[] { new NewEntryLine("hi", "Hello, world!") },
			"=hi Hello, world!"
		);
	}
	public void testParseNewEntryLineAndCrLf() {
		testParsesAs(
			new Chunk[] { new NewEntryLine("file", "bar.txt") },
			"=file bar.txt\r\n"
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
	public void testParseHeaderComment() {
		for( String terminator : Arrays.asList("", "\n", "\r\n", "\n\n", "\r\n\r\n") ) {
			testParsesAs(
				new Chunk[] {},
				"# A comment line!" + terminator
			);
		}
	}
	public void testParseHeader() {
		testParsesAs(
			new Chunk[] { new Header("hi there", "foo bar") },
			"hi there: foo bar"
		);
	}
	public void testParseHeaderWithCrLf() {
		testParsesAs(
			new Chunk[] { new Header("hi there", "foo bar") },
			"hi there: foo bar\r\n"
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
				new Header("hi:there", "foo bar\r\nbaz quux"),
				new Header("bill", "ted"),
				new Header("empty", ""),
				new Header("empty", ""),
				new Header("kyanu", "\nhorsey"),
				new Header("kevin", "\n"),
			},
			"=foo bar baz\n"+
			"hi:there: foo bar\r\n"+
			"\tbaz quux\n"+
			"bill: ted\r\n"+
			"empty:\n"+
			"empty: \n"+
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
	public void testParseContentNewlineEof() {
		for( String newline : Arrays.asList("\r\n","\n") ) {
			testParsesAs(
				new Chunk[] {
					new ContentPiece("hello\n".getBytes(UTF8))
				},
				newline + "hello\n"
			);
		}
	}
	public void testParseContentNewlineNewEntry() {
		for( String newline : Arrays.asList("\r\n","\n") ) {
			testParsesAs(
				new Chunk[] {
					new ContentPiece("hello".getBytes(UTF8)),
					new NewEntryLine("foo", "bar"),
				},
				// Should content+CRLF+"=foo..." actually omit the CR from content, also?
				newline + "hello\n=foo bar"
			);
		}
	}
	public void testParseContentEof() {
		for( String newline : Arrays.asList("\r\n","\n") ) {
			testParsesAs(
				new Chunk[] {
					new ContentPiece("hello".getBytes(UTF8))
				},
				newline + "hello"
			);
		}
	}
	public void testParseCommentContentEof() {
		for( String commentLine : Arrays.asList("", "# A comment\n", "# A comment\r\n")) {
			for( String newline : Arrays.asList("\r\n","\n") ) {
				testParsesAs(
					new Chunk[] {
						new ContentPiece("hello:".getBytes(UTF8))
					},
					commentLine + newline + "hello:"
				);
			}
		}
	}
	public void testParseEntryEntry() {
		testParsesAs(
			new Chunk[] {
				new NewEntryLine("foo", "bar"),
				new NewEntryLine("baz", "quux")
			},
			"=foo bar\r\n"+
			"=baz quux\n"
		);
	}
	public void testParseEntryEntryContentEof() {
		testParsesAs(
			new Chunk[] {
				new NewEntryLine("foo", "bar"),
				new NewEntryLine("baz", "quux"),
				new ContentPiece("some content".getBytes(UTF8))
			},
			"=foo bar\r\n"+
			"=baz quux\r\n\r\n"+
			"some content"
		);
	}
	public void testParseFiole() {
		testParsesAs(
			new Chunk[] {
				new NewEntryLine("foo.txt", ""),
				new NewEntryLine("fiole", ""),
				new NewEntryLine("file", "foo.txt"),
				new NewEntryLine("file", "bar.txt"),
				new NewEntryLine("file", "baz.txt"),
				new ContentPiece("Hello, this is baz.txt\n".getBytes(UTF8))
			},
			"=foo.txt\n"+
			"=fiole\n"+
			"=file foo.txt\n"+
			"=file bar.txt\n"+
			"=file baz.txt\n"+
			"\n"+
			"Hello, this is baz.txt\n"
		);
	}
	
	public static void main(String[] args) {
		new TEFParserTest().testParseEntryWithContent();
	}
}
