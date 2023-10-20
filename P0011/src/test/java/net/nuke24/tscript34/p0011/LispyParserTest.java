package net.nuke24.tscript34.p0011;

import net.nuke24.tscript34.p0011.Danducer.DucerData;
import net.nuke24.tscript34.p0011.sexp.Atom;
import net.nuke24.tscript34.p0011.sexp.ConsPair;
import net.nuke24.tscript34.p0011.sexp.LiteralValue;
import net.nuke24.tscript34.p0011.sexp.Symbols;

public class LispyParserTest extends XXTestCase {
	@Override public void setUp() {
		this.debugStream = System.err;
	}
	
	protected void testParsesTo(Object[] expectedOutput, Token[] input) {
		testDucerOutput(expectedOutput, input, LispyParser.ROOT_PARSER);
	}
	
	public void testParseNumber() {
		DucerData<Token[], Object[]> parsed = LispyParser.ROOT_PARSER.process(new Token[] { new Token("123", LispyCharDecoder.MODE_BAREWORD) }, true);
		assertTrue(parsed.isDone);
		assertEquals(1, parsed.output.length);
		assertEquals(new Atom("123"), parsed.output[0]);
	}
	public void testParseLiteral() {
		DucerData<Token[], Object[]> parsed = LispyParser.ROOT_PARSER.process(new Token[] { new Token("123", LispyCharDecoder.MODE_QUOTED) }, true);
		assertTrue(parsed.isDone);
		assertEquals(1, parsed.output.length);
		assertEquals(new LiteralValue("123"), parsed.output[0]);
	}
	public void testParseList() {
		testParsesTo(new Object[] {
			new ConsPair(
				new Atom("123"),
				new ConsPair(
					new LiteralValue("123"),
					Symbols.NIL
				)
			)
		}, new Token[] {
			new Token("(", LispyCharDecoder.MODE_DELIMITER),
			new Token("123", LispyCharDecoder.MODE_BAREWORD),
			new Token("123", LispyCharDecoder.MODE_QUOTED),
			new Token(")", LispyCharDecoder.MODE_DELIMITER)
		});
	}
	
	public void testParseNestedLists() {
		testParsesTo(new Object[] { new ConsPair(
			new Atom("123"),
			new ConsPair(
				new ConsPair(
					new Atom("foo"),
					Symbols.NIL
				),
				new ConsPair(
					Symbols.NIL,
					Symbols.NIL
				)
			)
		)}, new Token[] {
			new Token("(", LispyCharDecoder.MODE_DELIMITER),
			new Token("123", LispyCharDecoder.MODE_BAREWORD),
			new Token("(", LispyCharDecoder.MODE_DELIMITER),
			new Token("foo", LispyCharDecoder.MODE_BAREWORD),
			new Token(")", LispyCharDecoder.MODE_DELIMITER),
			new Token("(", LispyCharDecoder.MODE_DELIMITER),
			new Token(")", LispyCharDecoder.MODE_DELIMITER),
			new Token(")", LispyCharDecoder.MODE_DELIMITER)
		});
	}
}
