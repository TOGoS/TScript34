package net.nuke24.tscript34.p0011;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;
import net.nuke24.tscript34.p0011.Danducer.DucerData;
import net.nuke24.tscript34.p0011.sexp.Atom;
import net.nuke24.tscript34.p0011.sexp.ConsPair;
import net.nuke24.tscript34.p0011.sexp.LiteralValue;
import net.nuke24.tscript34.p0011.sexp.Symbols;

public class ParserTest extends TestCase {
	public void testParseNumber() {
		DucerData<Token[], Object[]> parsed = LispyParser.ROOT_PARSER.process(new Token[] { new Token("123", LispyCharDecoder.MODE_BAREWORD) }, true);
		assertEquals(new Atom("123"), parsed.output[0]);
	}
	public void testParseLiteral() {
		DucerData<Token[], Object[]> parsed = LispyParser.ROOT_PARSER.process(new Token[] { new Token("123", LispyCharDecoder.MODE_QUOTED) }, true);
		assertEquals(new LiteralValue("123"), parsed.output[0]);
	}
	public void testParseList() {
		DucerData<Token[], Object[]> state = LispyParser.ROOT_PARSER.process(new Token[] {
			new Token("(", LispyCharDecoder.MODE_DELIMITER),
			new Token("123", LispyCharDecoder.MODE_BAREWORD),
			new Token("123", LispyCharDecoder.MODE_QUOTED),
			new Token(")", LispyCharDecoder.MODE_DELIMITER)
		}, true);
		List<Object> result = Arrays.asList(state.output);
		// assertTrue(state.isDone);
		assertEquals(Arrays.asList(new ConsPair(
			new Atom("123"),
			new ConsPair(
				new LiteralValue("123"),
				Symbols.NIL
			)
		)), result);
	}
	public void testParseNestedLists() {
		DucerData<Token[], Object[]> state = LispyParser.ROOT_PARSER.process(new Token[] {
			new Token("(", LispyCharDecoder.MODE_DELIMITER),
			new Token("123", LispyCharDecoder.MODE_BAREWORD),
			new Token("(", LispyCharDecoder.MODE_DELIMITER),
			new Token("foo", LispyCharDecoder.MODE_BAREWORD),
			new Token(")", LispyCharDecoder.MODE_DELIMITER),
			new Token("(", LispyCharDecoder.MODE_DELIMITER),
			new Token(")", LispyCharDecoder.MODE_DELIMITER),
			new Token(")", LispyCharDecoder.MODE_DELIMITER)
		}, true);
		List<Object> result = Arrays.asList(state.output);
		assertEquals(Arrays.asList(new ConsPair(
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
		)), result);
	}
}
