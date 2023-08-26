package net.nuke24.tscript34.p0011;

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
		static final int MODE_END = Tokenizer.MODE_END;
		
		static final int[] OPS_NONE = new int[0];
		static final int[] OPS_DELIMITER = Tokenizer.compileOps(new String[] {
			"acc = buffer.length",
			"if acc != 0 {",
			"  flush-token",
			"}",
			"mode = "+MODE_DELIMITER,
			"buffer.append current-char",
			"flush-token",
			"mode = "+MODE_INIT,
		});
		static final int[] OPS_BAREWORD_CHAR = Tokenizer.compileOps(new String[] {
			"buffer.append current-char",
		});
		static final int[] OPS_INIT_TO_BAREWORD = Tokenizer.compileOps(new String[] {
			"mode = "+MODE_BAREWORD,
			"buffer.append current-char",
		});
		static final int[] OPS_BAREWORD_TO_WHITESPACE = Tokenizer.compileOps(new String[] {
			"flush-token",
			"mode = "+MODE_INIT,
		});
		static final int[] OPS_BAREWORD_TO_END = Tokenizer.compileOps(new String[] {
			"flush-token",
			"mode = "+MODE_END,
		});
		static final int[] OPS_END = Tokenizer.compileOps(new String[] {
			"mode = "+MODE_END,
		});

		public int[] decode(int mode, int character) {
			switch(mode) {
			case MODE_INIT:
				switch( character ) {
				case ' ': case '\t':
					return OPS_NONE;
				case '(':
					return OPS_DELIMITER;
				case -1:
					return OPS_END;
				default:
					return OPS_INIT_TO_BAREWORD;
				}
			case MODE_BAREWORD:
				switch( character ) {
				case ' ': case '\t':
					return OPS_BAREWORD_TO_WHITESPACE;
				case -1:
					return OPS_BAREWORD_TO_END;
				default:
					return OPS_BAREWORD_CHAR;
				}
			default:
				throw new RuntimeException("Unknown mode: "+mode);
			}
		};
	};
	
	public void testBasicTokenization() {
		DucerData<CharSequence, Token[]> s = new Tokenizer(new TestCharDecoder()).process("", false);
		s = s.process("(", true);
		assertEquals(1, s.output.length);
		assertEquals(new Token("(",1), s.output[0]);
	}
	
	protected <O>
	DucerData<CharSequence, O[]>
	processChunked( DucerData<CharSequence, O[]> s, CharSequence input, boolean endOfInput, int seed ) {
		System.err.println();
		Random r = new Random(seed);
		StringBuilder remainingInput = new StringBuilder();
		ArrayList<O> outputs = new ArrayList<O>();
		int offset=0;
		while( offset < input.length() ) {
			int chunkLen = Math.max(1, Math.min(input.length() - offset, r.nextInt(4)));
			int chunkEnd = offset+chunkLen;
			boolean chunkIsEndOfInput = endOfInput && chunkEnd == input.length();
			CharSequence chunk = input.subSequence(offset, chunkEnd);
			System.err.println("Chunk: \""+chunk+"\"");
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
	
	public void testTokenizeBareword() {
		for( int seed=0; seed<10; ++seed ) {
			DucerData<CharSequence, Token[]> s = new Tokenizer(new TestCharDecoder()).process("", false);
			s = processChunked(s, "(foo", true, seed);
			assertEquals(2, s.output.length);
			assertEquals(new Token("(",TestCharDecoder.MODE_DELIMITER), s.output[0]);
			assertEquals(new Token("foo",TestCharDecoder.MODE_BAREWORD), s.output[1]);
		}
	}
}
