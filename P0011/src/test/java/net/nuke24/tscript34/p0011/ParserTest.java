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
	static class Parser implements Danducer<Token[], Object[]> {
		// Could have separate classes for root and list parsers
		static final Object[] EMPTY_OUTPUT = new Object[0];
		static final Token[] EMPTY_TOKEN_LIST = new Token[0];
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
		
		DucerData<Token[], Object[]> itemResult(Object value, boolean endOfInput) {
			return this.isRoot() ?
				new DucerData<Token[], Object[]>(
					this,
					EMPTY_TOKEN_LIST,
					new Object[] {value},
					endOfInput
				) :
				new DucerData<Token[], Object[]>(
					this.add(value),
					EMPTY_TOKEN_LIST,
					EMPTY_OUTPUT,
					endOfInput
				);
		}
		
		DucerData<Token[], Object[]> eolResult(Object value, boolean endOfInput) {
			return parent.itemResult(value, endOfInput);
		}
		
		public DucerData<Token[], Object[]> processOne(Token input) {
			switch( input.mode ) {
			case LispyCharDecoder.MODE_DELIMITER:
				if( "(".equals(input.text) ) {
					return new DucerData<Token[], Object[]>(
						new Parser(this, Symbols.NIL),
						EMPTY_TOKEN_LIST,
						EMPTY_OUTPUT,
						false
					);
				} else if( ")".equals(input.text) ) {
					return eolResult(getResultList(), false);
				} else {
					throw new RuntimeException(new EvalException("What kind of delimiter is '"+input.text+"'?", input));
				}
			case LispyCharDecoder.MODE_LINE_COMMENT:
				// Ignore
				return new DucerData<Token[], Object[]>(
					this,
					EMPTY_TOKEN_LIST,
					EMPTY_OUTPUT,
					false
				);
			case LispyCharDecoder.MODE_BAREWORD:
				return itemResult(new Atom(input.text, input), false);
			case LispyCharDecoder.MODE_QUOTED:
				return itemResult(new LiteralValue(input.text, input), false);
			default:
				throw new RuntimeException(new EvalException("Did not expect token type "+input.mode, input));
			}
		}
		
		DucerData<Token[], Object[]> processEndOfInput(DucerData<Token[], Object[]> state) {
			// TODO: Could do some assertiosn here,
			// throw exception if we're not root.
			return state;
		}
		
		protected static <T> T[] join(T[] a, T[] b, Class<T[]> arrClass) {
			if( b.length == 0 ) return a;
			if( a.length == 0 ) return b;
			T[] c;
			try {
				c = arrClass.getDeclaredConstructor(Integer.class).newInstance(a.length + b.length);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			for( int i=0; i<a.length; ++i ) c[i] = a[i];
			for( int i=0; i<b.length; ++i ) c[i+a.length] = b[i];
			return c;
		}
		
		protected static <A,B> DucerData<A, B[]> update(DucerData<A,B[]> prev, DucerData<A,B[]> next, Class<B[]> bArrClass) {
			if( prev.output.length == 0 ) return next;
			// Could assert about !prev.isDone or something;
			// prev.isDone => next.output.length == 0
			// prev.isDone => next.isDone
			// Not sure if we can say anything about remaining inputs;
			// that should have been taken care of by the caller.
			return new DucerData<A,B[]>(next.state, next.remainingInput, join(prev.output, next.output, bArrClass), next.isDone);
		}
		
		@Override
		public DucerData<Token[], Object[]> process(Token[] input, boolean endOfInput) {
			DucerData<Token[], Object[]> data = new DucerData<>(this, EMPTY_TOKEN_LIST, EMPTY_OUTPUT, false);
			for( int i=0; i<input.length; ++i ) {
				DucerData<Token[], Object[]> newData = ((Parser)data.state).processOne(input[i]);
				assert(newData.remainingInput.length == 0);
				data = update(data, newData, Object[].class);
			}
			if( endOfInput ) {
				data = ((Parser)data.state).processEndOfInput(data);
			}
			return data;
		}
	}
	
	public void testParseNumber() {
		DucerData<Token[], Object[]> parsed = Parser.ROOT_PARSER.process(new Token[] { new Token("123", LispyCharDecoder.MODE_BAREWORD) }, true);
		assertEquals(new Atom("123"), parsed.output[0]);
	}
	public void testParseLiteral() {
		DucerData<Token[], Object[]> parsed = Parser.ROOT_PARSER.process(new Token[] { new Token("123", LispyCharDecoder.MODE_QUOTED) }, true);
		assertEquals(new LiteralValue("123"), parsed.output[0]);
	}
	public void testParseList() {
		DucerData<Token[], Object[]> state = Parser.ROOT_PARSER.process(new Token[] {
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
		DucerData<Token[], Object[]> state = Parser.ROOT_PARSER.process(new Token[] {
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
