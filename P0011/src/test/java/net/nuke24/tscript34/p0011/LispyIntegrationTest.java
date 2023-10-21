package net.nuke24.tscript34.p0011;

import net.nuke24.tscript34.p0011.Danducer.DucerData;
import net.nuke24.tscript34.p0011.sexp.Atom;
import net.nuke24.tscript34.p0011.sexp.ConsPair;
import net.nuke24.tscript34.p0011.sexp.LiteralValue;
import net.nuke24.tscript34.p0011.sexp.Symbols;

public class LispyIntegrationTest extends XXTestCase {
	public void testParseNilSource() {
		String source = "()";
		Tokenizer t = new Tokenizer(LispyCharDecoder.instance, "test.sexp");
		LispyParser p = LispyParser.ROOT_PARSER;
		DucerData<CharSequence,Token[]> tokenizationResult = t.process(source, true);
		DucerData<Token[],Object[]> parseResult = p.process(tokenizationResult.output, true);
		assertArrayEquals(
			new Object[] {
				new Atom(Symbols.S_NIL, "test.sexp", 0, 0, 0, 2),
			}, parseResult.output
		);
	}
	
	public void testParseListSource() {
		String source = "(foo bar \"ba\\tz\")";
		Tokenizer t = new Tokenizer(LispyCharDecoder.instance, "test.sexp");
		LispyParser p = LispyParser.ROOT_PARSER;
		DucerData<CharSequence,Token[]> tokenizationResult = t.process(source, true);
		DucerData<Token[],Object[]> parseResult = p.process(tokenizationResult.output, true);
		assertArrayEquals(
			new Object[] {
				new ConsPair(
					new Atom("foo", "test.sexp", 0, 1, 0, 4),
					new ConsPair(
						new Atom("bar", "test.sexp", 0, 5, 0, 8),
						new ConsPair(
							new LiteralValue("ba\tz", "test.sexp", 0, 9, 0, 16),
							new Atom(Symbols.S_NIL, "test.sexp", 0, 16, 0, 17),
							"test.sexp", 0, 9, 0, 17
						),
						"test.sexp", 0, 5, 0, 17
					),
					"test.sexp", 0, 0, 0, 17
				)
			}, parseResult.output
		);
	}
}
