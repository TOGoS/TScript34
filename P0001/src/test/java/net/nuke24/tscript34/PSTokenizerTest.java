package net.nuke24.tscript34;

import java.io.IOException;
import java.io.StringReader;

import junit.framework.TestCase;

public class PSTokenizerTest extends TestCase {
	public void testParseNothing() throws IOException {
		StringReader r = new StringReader("");
		PSTokenizer parser = new PSTokenizer(r, "testParseNothing", 1, 1);
		Token token = parser.readToken();
		assertEquals(
			// EOF is 'zero-width', so start and end source locations should be the same
			new Token(Token.QuoteStyle.EOF, "", "testParseNothing", 1, 1, 1, 1),
			token
		);
	}
	
	public void testParseBareword() throws IOException {
		StringReader r = new StringReader("foo 123 %comment foo bar");
		PSTokenizer parser = new PSTokenizer(r, "testParseBareword", 1, 1);
		assertEquals(
			new Token(Token.QuoteStyle.BAREWORD, "foo", "testParseBareword", 1, 1, 1, 4),
			parser.readToken()
		);
		assertEquals(
			new Token(Token.QuoteStyle.BAREWORD, "123", "testParseBareword", 1, 5, 1, 8),
			parser.readToken()
		);
		assertEquals(
			new Token(Token.QuoteStyle.HASH_COMMENT, "comment foo bar", "testParseBareword", 1, 9, 1, 25),
			parser.readToken()
		);
		assertEquals(
			new Token(Token.QuoteStyle.EOF, "", "testParseBareword", 1, 25, 1, 25),
			parser.readToken()
		);
	}
	
	public void testParseString() throws IOException {
		StringReader r = new StringReader("(foo 123) (456) /baz");
		PSTokenizer parser = new PSTokenizer(r, "testParseString", 1, 1);
		assertEquals(
			new Token(Token.QuoteStyle.LITERAL_STRING, "foo 123", "testParseString", 1, 1, 1, 10),
			parser.readToken()
		);
		assertEquals(
			new Token(Token.QuoteStyle.LITERAL_STRING, "456", "testParseString", 1, 11, 1, 16),
			parser.readToken()
		);
		assertEquals(
			new Token(Token.QuoteStyle.LITERAL_WORD, "baz", "testParseString", 1, 17, 1, 21),
			parser.readToken()
		);
		assertEquals(
			new Token(Token.QuoteStyle.EOF, "", "testParseString", 1, 21, 1, 21),
			parser.readToken()
		);
	}
}
