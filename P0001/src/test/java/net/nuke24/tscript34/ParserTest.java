package net.nuke24.tscript34;

import java.io.IOException;
import java.io.StringReader;

import junit.framework.TestCase;
import net.nuke24.tscript34.Parser;
import net.nuke24.tscript34.Token;

public class ParserTest extends TestCase {
	public void testParseNothing() throws IOException {
		StringReader r = new StringReader("");
		Parser parser = new Parser(r, "testParseNothing", 1, 1);
		Token token = parser.readToken();
		assertEquals(
			// EOF is 'zero-width', so start and end source locations should be the same
			new Token(Token.QuoteStyle.EOF, "", "testParseNothing", 1, 1, 1, 1),
			token
		);
	}
	
	public void testParseBareword() throws IOException {
		StringReader r = new StringReader("foo 123 %comment");
		Parser parser = new Parser(r, "testParseBareword", 1, 1);
		Token token = parser.readToken();
		assertEquals(
			// EOF is 'zero-width', so start and end source locations should be the same
			new Token(Token.QuoteStyle.BAREWORD, "foo", "testParseBareword", 1, 1, 1, 4),
			token
		);
		
	}
}
