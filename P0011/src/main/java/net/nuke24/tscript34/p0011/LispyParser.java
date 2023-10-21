package net.nuke24.tscript34.p0011;

import net.nuke24.tscript34.p0011.sexp.Atom;
import net.nuke24.tscript34.p0011.sexp.ConsPair;
import net.nuke24.tscript34.p0011.sexp.LiteralValue;
import net.nuke24.tscript34.p0011.sexp.Symbols;
import net.nuke24.tscript34.p0011.sloc.HasSourceLocation;

class LispyParser implements Danducer<Token[], Object[]> {
	// Could have separate classes for root and list parsers
	static final Object[] EMPTY_OUTPUT = new Object[0];
	static final Token[] EMPTY_TOKEN_LIST = new Token[0];
	static final LispyParser ROOT_PARSER = new LispyParser(null, null);
	
	final LispyParser parent;
	final HasSourceLocation currentList; // ((a . b) . c) ...
	
	public LispyParser(LispyParser parent, HasSourceLocation currentList) {
		this.parent = parent;
		this.currentList = currentList;
	}
	
	boolean isRoot() {
		return this.parent == null;
	}
	
	static HasSourceLocation withSourceLocation(Object obj, String fn, int li, int ci, int eli, int eci) {
		if( obj instanceof ConsPair ) {
			return new ConsPair(((ConsPair)obj).left, ((ConsPair)obj).right, fn, li, ci, eli, eci);
		} else if( obj instanceof Atom ) {
			return new Atom(((Atom)obj).text, fn, li, ci, eli, eci);
		} else {
			throw new RuntimeException("Lol don't know how to recreate-with-different-source-location a "+obj.getClass());
		}
	}
	
	protected Object getResultList(HasSourceLocation endDelimiter) {
		HasSourceLocation cons = new Atom(Symbols.S_NIL, endDelimiter);
		HasSourceLocation c = this.currentList;
		while( !Symbols.isNil(c) ) {
			ConsPair p = ((ConsPair)c);
			HasSourceLocation leftSl = (HasSourceLocation)p.right;
			cons = new ConsPair(p.right, cons, cons.getSourceFileUri(), leftSl.getSourceLineIndex(), leftSl.getSourceColumnIndex(), cons.getSourceEndLineIndex(), cons.getSourceEndColumnIndex());
			c = (HasSourceLocation)p.left;
		}
		// Include the leading '(' as part of the outermost
		return withSourceLocation(cons, c.getSourceFileUri(),
			c.getSourceLineIndex(), c.getSourceColumnIndex(),
			cons.getSourceEndLineIndex(), cons.getSourceEndColumnIndex());
	}
	
	LispyParser add(Object item) {
		return new LispyParser(this.parent, new ConsPair(this.currentList, item));
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
					new LispyParser(this, new Atom(Symbols.S_NIL, input)),
					EMPTY_TOKEN_LIST,
					EMPTY_OUTPUT,
					false
				);
			} else if( ")".equals(input.text) ) {
				return eolResult(getResultList(input), false);
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
		if( this.parent != null ) {
			throw new RuntimeException("Unexpected eof");
		}
		// If no more input, then there will be no more output!
		return state.withIsDone(true);
	}
	
	protected static <A,B> DucerData<A, B[]> update(DucerData<A,B[]> prev, DucerData<A,B[]> next) {
		if( prev.output.length == 0 ) return next;
		// Could assert about !prev.isDone or something;
		// prev.isDone => next.output.length == 0
		// prev.isDone => next.isDone
		// Not sure if we can say anything about remaining inputs;
		// that should have been taken care of by the caller.
		return new DucerData<A,B[]>(next.state, next.remainingInput, ArrayUtil.join(prev.output, next.output), next.isDone);
	}
	
	@Override
	public DucerData<Token[], Object[]> process(Token[] input, boolean endOfInput) {
		DucerData<Token[], Object[]> data = new DucerData<>(this, EMPTY_TOKEN_LIST, EMPTY_OUTPUT, false);
		for( int i=0; i<input.length; ++i ) {
			DucerData<Token[], Object[]> newData = ((LispyParser)data.state).processOne(input[i]);
			assert(newData.remainingInput.length == 0);
			data = update(data, newData);
		}
		if( endOfInput ) {
			data = ((LispyParser)data.state).processEndOfInput(data);
		}
		return data;
	}
}
