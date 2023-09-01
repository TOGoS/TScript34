package net.nuke24.tscript34.p0011;

import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Random;

import junit.framework.TestCase;
import net.nuke24.tscript34.p0011.Danducer.DucerData;
import net.nuke24.tscript34.p0011.Tokenizer.CharDecoder;
import net.nuke24.tscript34.p0011.Tokenizer.Token;

public class TokenizerTest extends TestCase {
	static class TestCharDecoder implements CharDecoder {
		static final int MODE_INIT = Tokenizer.MODE_DEFAULT;
		static final int MODE_DELIMITER = 1;
		static final int MODE_BAREWORD = 2;
		static final int MODE_LINE_COMMENT = 3;
		static final int MODE_QUOTED = 4;
		static final int MODE_QUOTED_ESCAPE = 5;
		static final int MODE_END = Tokenizer.MODE_END;
		
		// INIT mode ops
		static final int[] OPS_NONE = new int[0];
		static final int[] OPS_INIT = Tokenizer.compileOps(new String[] {
			"mode = "+MODE_INIT,
		});
		static final int[] OPS_DELIMITER = Tokenizer.compileOps(new String[] {
			"jump-to-mode "+MODE_DELIMITER
		});
		static final int[] OPS_QUOTE = Tokenizer.compileOps(new String[] {
			"mode = "+MODE_QUOTED,
		});
		static final int[] OPS_BAREWORD = Tokenizer.compileOps(new String[] {
			"jump-to-mode "+MODE_BAREWORD,
		});
		static final int[] OPS_LINE_COMMENT = Tokenizer.compileOps(new String[] {
			"mode = "+MODE_LINE_COMMENT,
		});
		
		static final int[] OPS_APPEND_CHAR = Tokenizer.compileOps(new String[] {
			"buffer.append current-char",
		});
		static final int[] OPS_APPEND_CHAR_AND_RETURN_TO_QUOTED = Tokenizer.compileOps(new String[] {
			"buffer.append current-char",
			"mode = "+MODE_QUOTED
		});
		static final int[] OPS_BAREWORD_TO_DELIMITER = Tokenizer.compileOps(new String[] {
			"flush-token",
			"jump-to-mode "+MODE_DELIMITER,
		});
		static final int[] OPS_END_BAREWORD = Tokenizer.compileOps(new String[] {
			"flush-token",
			"jump-to-mode "+MODE_INIT,
			//"mode = "+MODE_INIT,
		});
		
		static final int[] OPS_DELIMITER_CHAR = Tokenizer.compileOps(new String[] {
			"buffer.append current-char",
			"flush-token",
			"mode = "+MODE_INIT,
		});
		
		static final int[] OPS_QUOTED_ESCAPE = Tokenizer.compileOps(new String[] {
			"mode = "+MODE_QUOTED_ESCAPE,
		});
		static final int[] OPS_END_QUOTE = Tokenizer.compileOps(new String[] {
			"flush-token",
			"mode = "+MODE_INIT,
		});	
		
		static final int[] OPS_END = Tokenizer.compileOps(new String[] {
			"mode = "+MODE_END,
		});
		
		public int[] decode(int mode, int character) {
			switch(mode) {
			case MODE_INIT:
				switch( character ) {
				case '#':
					return OPS_LINE_COMMENT;
				case ' ': case '\t': case '\r': case '\n':
					return OPS_NONE;
				case '(': case ')':
					return OPS_DELIMITER;
				case '"':
					return OPS_QUOTE;
				case -1:
					return OPS_END;
				default:
					return OPS_BAREWORD;
				}
			case MODE_BAREWORD:
				switch( character ) {
				// In theory, should be able to just flush the token,
				// reject the character, and let MODE_INIT handle it.
				
				case ' ': case '\t': case '\r': case '\n':
				case '(': case ')':
				case '"':
				case -1:
					return OPS_END_BAREWORD;
				default:
					return OPS_APPEND_CHAR;
				}
			case MODE_QUOTED:
				switch( character ) {
				case '"':
					return OPS_END_QUOTE;
				case '\\':
					return OPS_QUOTED_ESCAPE;
				default:
					return OPS_APPEND_CHAR;
				}
			case MODE_QUOTED_ESCAPE:
				switch( character ) {
				case '"': case '\\':
					return OPS_APPEND_CHAR_AND_RETURN_TO_QUOTED;
				default:
					throw new RuntimeException("Unrecognized escape sequence: \"\\"+(char)character+"\"");
				}
			case MODE_LINE_COMMENT:
				switch( character ) {
				case '\n':
					return OPS_INIT;
				case -1:
					return OPS_END;
				default:
					return OPS_NONE;
				}
			case MODE_DELIMITER:
				return OPS_DELIMITER_CHAR;
			default:
				throw new RuntimeException("Unknown mode: "+mode);
			}
		};
	};
	
	PrintStream debugStream;
	@Override public void setUp() {
		//this.debugStream = System.err;
	}
	
	public void testBasicTokenization() {
		DucerData<CharSequence, Token[]> s = new Tokenizer(new TestCharDecoder()).process("", false);
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
			DucerData<CharSequence, Token[]> s = new Tokenizer(new TestCharDecoder(), sourceFilename).withDebugStream(debugStream).process("", false);
			s = processChunked(s, text, true, seed);
			assertArrayEquals(expected, s.output);
		}
	}
	protected void testTokenizesTo(Token[] expected, String text) {
		testTokenizesTo(expected, text, null);
	}
	
	public void testTokenizeBareword() {
		testTokenizesTo(new Token[] {
			new Token("(",TestCharDecoder.MODE_DELIMITER),
			new Token("foo",TestCharDecoder.MODE_BAREWORD),
		}, "(foo");
	}
	
	public void testTokenizeBarewordAndLineComment() {
		testTokenizesTo(new Token[] {
			new Token("(",TestCharDecoder.MODE_DELIMITER),
			new Token("foo",TestCharDecoder.MODE_BAREWORD),
		}, "( foo # bar");
	}
	
	public void testTokenizeBarewordAndMoreLineComments() {
		testTokenizesTo(new Token[] {
			new Token("(",TestCharDecoder.MODE_DELIMITER),
			new Token("foo",TestCharDecoder.MODE_BAREWORD),
		},
			"#!/bin/foobar\n" +
			"( foo # bar\n" +
			"# baz\n"
		);
	}
	public void testTokenizeMoreStuff() {
		testTokenizesTo(new Token[] {
			new Token("(",TestCharDecoder.MODE_DELIMITER),
			new Token("foo",TestCharDecoder.MODE_BAREWORD),
			new Token(".",TestCharDecoder.MODE_BAREWORD),
			new Token("(",TestCharDecoder.MODE_DELIMITER),
			new Token("bar",TestCharDecoder.MODE_BAREWORD),
			new Token(".",TestCharDecoder.MODE_BAREWORD),
			new Token("(",TestCharDecoder.MODE_DELIMITER),
			new Token(")",TestCharDecoder.MODE_DELIMITER),
			new Token(")",TestCharDecoder.MODE_DELIMITER),
			new Token(")",TestCharDecoder.MODE_DELIMITER),
		},
			"#!/bin/foobar\n" +
			"(foo . (bar . ()))\n"
		);
	}
	public void testTokenizeQuotedString() {
		testTokenizesTo(new Token[] {
			new Token("(",TestCharDecoder.MODE_DELIMITER),
			new Token("foo (bar)",TestCharDecoder.MODE_QUOTED),
			new Token(")",TestCharDecoder.MODE_DELIMITER),
		},
			"#!/bin/foobar\n" +
			"(\"foo (bar)\")"
		);
	}
	public void testTokenizeQuotedStringWithEscape() {
		testTokenizesTo(new Token[] {
			new Token("(",TestCharDecoder.MODE_DELIMITER),
			new Token("foo \"(bar)\"",TestCharDecoder.MODE_QUOTED),
			new Token(")",TestCharDecoder.MODE_DELIMITER),
		},
			"#!/bin/foobar\n" +
			"(\"foo \\\"(bar)\\\"\")"
		);
	}
	public void testTokenizeWithSourceLocation() {
		testTokenizesTo(new Token[] {
			new Token("foo",TestCharDecoder.MODE_BAREWORD, "foo.txt", 1, 0, 1, 3),
			new Token("(",TestCharDecoder.MODE_DELIMITER , "foo.txt", 1, 4, 1, 5),
			new Token("bar",TestCharDecoder.MODE_BAREWORD, "foo.txt", 1, 5, 1, 8),
			new Token(")",TestCharDecoder.MODE_DELIMITER , "foo.txt", 1, 8, 1, 9),
		},
			"#!/bin/foobar\n" +
			"foo (bar)",
			"foo.txt"
		);
	}
}
