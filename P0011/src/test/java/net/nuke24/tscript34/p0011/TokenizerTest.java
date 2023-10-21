package net.nuke24.tscript34.p0011;

import java.io.PrintStream;

import net.nuke24.tscript34.p0011.Danducer.DucerData;
import net.nuke24.tscript34.p0011.DanducerTestUtil.CharSequenceChunkerator;

public class TokenizerTest extends XXTestCase {
	PrintStream debugStream;
	@Override public void setUp() {
		this.debugStream = System.err;
	}
	
	public void testBasicTokenization() {
		DucerData<CharSequence, Token[]> s = new Tokenizer(new LispyCharDecoder()).process("", false);
		s = s.process("(", true);
		assertEquals(1, s.output.length);
		assertEquals(new Token("(",1), s.output[0]);
	}
		
	protected void testTokenizesTo(Token[] expected, String text, String sourceUri) {
		DucerData<CharSequence,Token[]> state = new Tokenizer(new LispyCharDecoder(), sourceUri).withDebugStream(debugStream).process("", false);
		testDucerOutput(expected, new CharSequenceChunkerator(text), state);
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
			new Token("foo \"(bar)\"\t",LispyCharDecoder.MODE_QUOTED),
			new Token(")",LispyCharDecoder.MODE_DELIMITER),
		},
			"#!/bin/foobar\n" +
			"(\"foo \\\"(bar)\\\"\\t\")"
		);
	}
	
	public void testTokenizeWithSourceLocation() {
		testTokenizesTo(new Token[] {
			new Token("foo",LispyCharDecoder.MODE_BAREWORD, "file:foo.txt", 1, 0, 1, 3),
			new Token("(",LispyCharDecoder.MODE_DELIMITER , "file:foo.txt", 1, 4, 1, 5),
			new Token("bar",LispyCharDecoder.MODE_BAREWORD, "file:foo.txt", 1, 5, 1, 8),
			new Token(")",LispyCharDecoder.MODE_DELIMITER , "file:foo.txt", 1, 8, 1, 9),
		},
			"#!/bin/foobar\n" +
			"foo (bar)",
			"file:foo.txt"
		);
	}
}
