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
	
	public void testParseHashComment() throws IOException {
		StringReader r = new StringReader("# foo bar baz");
		PSTokenizer parser = new PSTokenizer(r, "testParseHashComment", 1, 1);
		assertEquals(
			new Token(Token.QuoteStyle.HASH_COMMENT, "foo bar baz", "testParseHashComment", 1, 1, 1, 14),
			parser.readToken()
		);
		assertEquals(
			new Token(Token.QuoteStyle.EOF, "", "testParseHashComment", 1, 14, 1, 14),
			parser.readToken()
		);
	}
	
	public void testParseShebangComment() throws IOException {
		StringReader r = new StringReader("#!foo bar baz");
		PSTokenizer parser = new PSTokenizer(r, "testParseShebangComment", 1, 1);
		assertEquals(
			new Token(Token.QuoteStyle.SHEBANG_COMMENT, "foo bar baz", "testParseShebangComment", 1, 1, 1, 14),
			parser.readToken()
		);
		assertEquals(
			new Token(Token.QuoteStyle.EOF, "", "testParseShebangComment", 1, 14, 1, 14),
			parser.readToken()
		);
	}
	
	public void testParseNotComment() throws IOException {
		StringReader r = new StringReader("#foo bar baz");
		PSTokenizer parser = new PSTokenizer(r, "testParseNotComment", 1, 1);
		assertEquals(
			new Token(Token.QuoteStyle.BAREWORD, "#foo", "testParseNotComment", 1, 1, 1, 5),
			parser.readToken()
		);
		assertEquals(
			new Token(Token.QuoteStyle.BAREWORD, "bar", "testParseNotComment", 1, 6, 1, 9),
			parser.readToken()
		);
		assertEquals(
			new Token(Token.QuoteStyle.BAREWORD, "baz", "testParseNotComment", 1, 10, 1, 13),
			parser.readToken()
		);
		assertEquals(
			new Token(Token.QuoteStyle.EOF, "", "testParseNotComment", 1, 13, 1, 13),
			parser.readToken()
		);
	}
	
	public void testParseSquareBrackets() throws IOException {
		StringReader r = new StringReader("[foo bar]");
		PSTokenizer parser = new PSTokenizer(r, "testParseSquareBrackets", 1, 1);
		
		assertEquals(
			new Token(Token.QuoteStyle.BAREWORD, "[", "testParseSquareBrackets", 1, 1, 1, 2),
			parser.readToken()
		);
		assertEquals(
			new Token(Token.QuoteStyle.BAREWORD, "foo", "testParseSquareBrackets", 1, 2, 1, 5),
			parser.readToken()
		);
		assertEquals(
			new Token(Token.QuoteStyle.BAREWORD, "bar", "testParseSquareBrackets", 1, 6, 1, 9),
			parser.readToken()
		);
		assertEquals(
			new Token(Token.QuoteStyle.BAREWORD, "]", "testParseSquareBrackets", 1, 9, 1, 10),
			parser.readToken()
		);
		assertEquals(
			new Token(Token.QuoteStyle.EOF, "", "testParseSquareBrackets", 1, 10, 1, 10),
			parser.readToken()
		);
	}
	
	public void testParseCurlyBrackets() throws IOException {
		StringReader r = new StringReader("{foo bar}");
		PSTokenizer parser = new PSTokenizer(r, "testParseCurlyBrackets", 1, 1);
		
		assertEquals(
			new Token(Token.QuoteStyle.BAREWORD, "{", "testParseCurlyBrackets", 1, 1, 1, 2),
			parser.readToken()
		);
		assertEquals(
			new Token(Token.QuoteStyle.BAREWORD, "foo", "testParseCurlyBrackets", 1, 2, 1, 5),
			parser.readToken()
		);
		assertEquals(
			new Token(Token.QuoteStyle.BAREWORD, "bar", "testParseCurlyBrackets", 1, 6, 1, 9),
			parser.readToken()
		);
		assertEquals(
			new Token(Token.QuoteStyle.BAREWORD, "}", "testParseCurlyBrackets", 1, 9, 1, 10),
			parser.readToken()
		);
		assertEquals(
			new Token(Token.QuoteStyle.EOF, "", "testParseCurlyBrackets", 1, 10, 1, 10),
			parser.readToken()
		);
	}
}
