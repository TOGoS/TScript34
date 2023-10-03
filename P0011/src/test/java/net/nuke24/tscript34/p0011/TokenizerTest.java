package net.nuke24.tscript34.p0011;

import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Random;

import junit.framework.TestCase;
import net.nuke24.tscript34.p0011.Danducer.DucerData;
import net.nuke24.tscript34.p0011.Tokenizer.Token;

public class TokenizerTest extends TestCase {
	PrintStream debugStream;
	@Override public void setUp() {
		//this.debugStream = System.err;
	}
	
	public void testBasicTokenization() {
		DucerData<CharSequence, Token[]> s = new Tokenizer(new LispyCharDecoder()).process("", false);
		s = s.process("(", true);
		assertEquals(1, s.output.length);
		assertEquals(new Token("(",1), s.output[0]);
	}
	
	protected <O>
	DucerData<CharSequence, O[]>
	processChunked( DucerData<CharSequence, O[]> s, CharSequence input, boolean endOfInput, int seed ) {
		if(debugStream != null) debugStream.println();
		Random r = new Random(seed);
		StringBuilder remainingInput = new StringBuilder();
		ArrayList<O> outputs = new ArrayList<O>();
		int offset=0;
		while( offset < input.length() ) {
			int chunkLen = Math.max(1, Math.min(input.length() - offset, r.nextInt(4)));
			int chunkEnd = offset+chunkLen;
			boolean chunkIsEndOfInput = endOfInput && chunkEnd == input.length();
			CharSequence chunk = input.subSequence(offset, chunkEnd);
			if(debugStream != null) debugStream.println("Chunk: \""+chunk+"\"");
			s = s.process(chunk, chunkIsEndOfInput);
			for( int i=0; i<s.output.length; ++i ) {
				outputs.add(s.output[i]);
			}
			offset = chunkEnd;
		}
		@SuppressWarnings("unchecked")
		O[] outputArr = (O[])Array.newInstance(s.output.getClass().getComponentType(), outputs.size());
		return new DucerData<CharSequence, O[]>(
			s.state, remainingInput.toString(),
			outputs.toArray(outputArr),
			s.isDone
		); 
		
	}
	
	protected <T> String arrayToString(T[] arr) {
		StringBuilder sb = new StringBuilder("[");
		String sep = "";
		for( T item : arr ) {
			sb.append(sep);
			sb.append(item);
			sep = " ";
		}
		sb.append("]");
		return sb.toString();
	}
	
	protected <T> void assertArrayEquals(T[] expected, T[] actual) {
		String expectedStr = arrayToString(expected);
		String actualStr = arrayToString(actual);
		if( !expectedStr.equals(actualStr) ) {
			System.err.println("Expected: "+expectedStr);
			System.err.println("Actual  : "+actualStr);
		}
		assertEquals(expected.length, actual.length);
		for( int i=0; i<expected.length; ++i ) {
			assertEquals(expected[i], actual[i]);
		}
	}
	
	protected void testTokenizesTo(Token[] expected, String text, String sourceFilename) {
		for( int seed=0; seed<10; ++seed ) {
			DucerData<CharSequence, Token[]> s = new Tokenizer(new LispyCharDecoder(), sourceFilename).withDebugStream(debugStream).process("", false);
			s = processChunked(s, text, true, seed);
			assertArrayEquals(expected, s.output);
		}
	}
	protected void testTokenizesTo(Token[] expected, String text) {
		testTokenizesTo(expected, text, null);
	}
	
	public void testTokenizeBareword() {
		testTokenizesTo(new Token[] {
			new Token("(",LispyCharDecoder.MODE_DELIMITER),
			new Token("foo",LispyCharDecoder.MODE_BAREWORD),
		}, "(foo");
	}
	
	public void testTokenizeBarewordAndLineComment() {
		testTokenizesTo(new Token[] {
			new Token("(",LispyCharDecoder.MODE_DELIMITER),
			new Token("foo",LispyCharDecoder.MODE_BAREWORD),
		}, "( foo # bar");
	}
	
	public void testTokenizeBarewordAndMoreLineComments() {
		testTokenizesTo(new Token[] {
			new Token("(",LispyCharDecoder.MODE_DELIMITER),
			new Token("foo",LispyCharDecoder.MODE_BAREWORD),
		},
			"#!/bin/foobar\n" +
			"( foo # bar\n" +
			"# baz\n"
		);
	}
	public void testTokenizeMoreStuff() {
		testTokenizesTo(new Token[] {
			new Token("(",LispyCharDecoder.MODE_DELIMITER),
			new Token("foo",LispyCharDecoder.MODE_BAREWORD),
			new Token(".",LispyCharDecoder.MODE_BAREWORD),
			new Token("(",LispyCharDecoder.MODE_DELIMITER),
			new Token("bar",LispyCharDecoder.MODE_BAREWORD),
			new Token(".",LispyCharDecoder.MODE_BAREWORD),
			new Token("(",LispyCharDecoder.MODE_DELIMITER),
			new Token(")",LispyCharDecoder.MODE_DELIMITER),
			new Token(")",LispyCharDecoder.MODE_DELIMITER),
			new Token(")",LispyCharDecoder.MODE_DELIMITER),
		},
			"#!/bin/foobar\n" +
			"(foo . (bar . ()))\n"
		);
	}
	public void testTokenizeQuotedString() {
		testTokenizesTo(new Token[] {
			new Token("(",LispyCharDecoder.MODE_DELIMITER),
			new Token("foo (bar)",LispyCharDecoder.MODE_QUOTED),
			new Token(")",LispyCharDecoder.MODE_DELIMITER),
		},
			"#!/bin/foobar\n" +
			"(\"foo (bar)\")"
		);
	}
	public void testTokenizeQuotedStringWithEscape() {
		testTokenizesTo(new Token[] {
			new Token("(",LispyCharDecoder.MODE_DELIMITER),
			new Token("foo \"(bar)\"",LispyCharDecoder.MODE_QUOTED),
			new Token(")",LispyCharDecoder.MODE_DELIMITER),
		},
			"#!/bin/foobar\n" +
			"(\"foo \\\"(bar)\\\"\")"
		);
	}
	public void testTokenizeWithSourceLocation() {
		testTokenizesTo(new Token[] {
			new Token("foo",LispyCharDecoder.MODE_BAREWORD, "foo.txt", 1, 0, 1, 3),
			new Token("(",LispyCharDecoder.MODE_DELIMITER , "foo.txt", 1, 4, 1, 5),
			new Token("bar",LispyCharDecoder.MODE_BAREWORD, "foo.txt", 1, 5, 1, 8),
			new Token(")",LispyCharDecoder.MODE_DELIMITER , "foo.txt", 1, 8, 1, 9),
		},
			"#!/bin/foobar\n" +
			"foo (bar)",
			"foo.txt"
		);
	}
}
