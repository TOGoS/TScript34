package net.nuke24.tscript34.p0011;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;
import net.nuke24.tscript34.p0011.Danducer.DucerData;
import net.nuke24.tscript34.p0011.sexp.Atom;
import net.nuke24.tscript34.p0011.sexp.ConsPair;
import net.nuke24.tscript34.p0011.sexp.LiteralValue;
import net.nuke24.tscript34.p0011.sexp.Symbols;

public class ParserTest extends TestCase {
	static class Parser implements Danducer<Token, Object[]> {
		// Could have separate classes for root and list parsers
		static final Object[] EMPTY_OUTPUT = new Object[0];
		static final Parser ROOT_PARSER = new Parser(null, null);
		
		final Parser parent;
		final Object currentList; // ((a . b) . c) ...
		
		public Parser(Parser parent, Object currentList) {
			this.parent = parent;
			this.currentList = currentList;
		}
		
		boolean isRoot() {
			return this.parent == null;
		}
		
		protected Object getResultList() {
			Object cons = Symbols.NIL;
			Object c = this.currentList;
			while( c != Symbols.NIL ) {
				ConsPair p = ((ConsPair)c);
				cons = new ConsPair(p.right, cons);
				c = p.left;
			}
			return cons;
		}
		
		Parser add(Object item) {
			return new Parser(this.parent, new ConsPair(this.currentList, item));
		}
		
		DucerData<Token, Object[]> itemResult(Object value, boolean endOfInput) {
			return this.isRoot() ?
				new DucerData<Token, Object[]>(
					this,
					null,
					new Object[] {value},
					endOfInput
				) :
				new DucerData<Token, Object[]>(
					this.add(value),
					null,
					EMPTY_OUTPUT,
					endOfInput
				);
		}
		
		DucerData<Token, Object[]> eolResult(Object value, boolean endOfInput) {
			return parent.itemResult(value, endOfInput);
		}
		
		@Override
		public DucerData<Token, Object[]> process(Token input, boolean endOfInput) {
			switch( input.mode ) {
			case LispyCharDecoder.MODE_DELIMITER:
				if( "(".equals(input.text) ) {
					return new DucerData<Token, Object[]>(
						new Parser(this, Symbols.NIL),
						null,
						EMPTY_OUTPUT,
						endOfInput
					);
				} else if( ")".equals(input.text) ) {
					return eolResult(getResultList(), endOfInput);
				} else {
					throw new RuntimeException(new EvalException("What kind of delimiter is '"+input.text+"'?", input));
				}
			case LispyCharDecoder.MODE_LINE_COMMENT:
				// Ignore
				return new DucerData<Token, Object[]>(
					this,
					null,
					EMPTY_OUTPUT,
					endOfInput
				);
			case LispyCharDecoder.MODE_BAREWORD:
				return itemResult(new Atom(input.text, input), endOfInput);
			case LispyCharDecoder.MODE_QUOTED:
				return itemResult(new LiteralValue(input.text, input), endOfInput);
			default:
				throw new RuntimeException(new EvalException("Did not expect token type "+input.mode, input));
			}
		}
	}
	
	public void testParseNumber() {
		DucerData<Token, Object[]> parsed = Parser.ROOT_PARSER.process(new Token("123", LispyCharDecoder.MODE_BAREWORD), true);
		assertEquals(new Atom("123"), parsed.output[0]);
	}
	public void testParseLiteral() {
		DucerData<Token, Object[]> parsed = Parser.ROOT_PARSER.process(new Token("123", LispyCharDecoder.MODE_QUOTED), true);
		assertEquals(new LiteralValue("123"), parsed.output[0]);
	}
	public void testParseList() {
		List<Object> result = new ArrayList<Object>();
		DucerData<Token, Object[]> state = new DucerData<Token,Object[]>(Parser.ROOT_PARSER, null, new Object[0], false);
		for( Token t : Arrays.asList(
			new Token("(", LispyCharDecoder.MODE_DELIMITER),
			new Token("123", LispyCharDecoder.MODE_BAREWORD),
			new Token("123", LispyCharDecoder.MODE_QUOTED),
			new Token(")", LispyCharDecoder.MODE_DELIMITER)
		)) {
			state = state.state.process(t, false);
			for( Object o : state.output ) result.add(o);
		}
		assertEquals(Arrays.asList(new ConsPair(
			new Atom("123"),
			new ConsPair(
				new LiteralValue("123"),
				Symbols.NIL
			)
		)), result);
	}
	public void testParseNestedEmptyList() {
		List<Object> result = new ArrayList<Object>();
		DucerData<Token, Object[]> state = new DucerData<Token,Object[]>(Parser.ROOT_PARSER, null, new Object[0], false);
		for( Token t : Arrays.asList(
			new Token("(", LispyCharDecoder.MODE_DELIMITER),
			new Token("123", LispyCharDecoder.MODE_BAREWORD),
			new Token("(", LispyCharDecoder.MODE_DELIMITER),
			new Token(")", LispyCharDecoder.MODE_DELIMITER),
			new Token(")", LispyCharDecoder.MODE_DELIMITER)
		)) {
			state = state.state.process(t, false);
			for( Object o : state.output ) result.add(o);
		}
		assertEquals(Arrays.asList(new ConsPair(
			new Atom("123"),
			new ConsPair(
				Symbols.NIL,
				Symbols.NIL
			)
		)), result);
	}
}
