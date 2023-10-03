package net.nuke24.tscript34.p0011;

import java.io.IOException;

import javax.script.ScriptException;

import junit.framework.TestCase;

import net.nuke24.tscript34.p0011.sexp.Atom;
import net.nuke24.tscript34.p0011.sexp.ConsPair;
import net.nuke24.tscript34.p0011.sexp.LiteralValue;
import net.nuke24.tscript34.p0011.sloc.HasSourceLocation;

class EvalException extends ScriptException {
	private static final long serialVersionUID = 3707037127440615004L;
	final HasSourceLocation sLoc;
	public EvalException(String message, HasSourceLocation sLoc) {
		super(message, sLoc.getSourceFileUri(), sLoc.getSourceLineIndex()+1, sLoc.getSourceColumnIndex()+1);
		this.sLoc = sLoc;
	}
}

interface Macro<T,R> {
	public R apply(T arg, Function<String,Object> defs) throws EvalException;
}
interface Function<T,R> {
	public R apply(T arg) throws EvalException;
}

class Evaluator {
	// Interpreter values:
	// (cons a b) = Pair(a, b)
	// () = null
	// foo = Symbol("foo")
	
	static final String S_NIL = "http://ns.nuke24.net/TScript34/P0011/Values/Nil";
	static final String S_MACRO = "http://ns.nuke24.net/TScript34/P0011/X/Macro";
	static final String S_QUOTE = "http://ns.nuke24.net/TScript34/P0011/Macro/Quote";
	static final String FN_CONCAT = "http://ns.nuke24.net/TOGVM/Functions/Concatenate";
	static final String FN_CONS = "http://ns.nuke24.net/TScript34/P0011/Functions/Cons";
	static final String FN_HEAD = "http://ns.nuke24.net/TScript34/P0011/Functions/Head";
	static final String FN_TAIL = "http://ns.nuke24.net/TScript34/P0011/Functions/Tail";
	
	static void format(Object obj, Appendable dest) throws IOException {
		dest.append(obj.toString());
	}
	
	static ConsPair cons(Object a, Object b) {
		return new ConsPair(a, b);
	}
	static HasSourceLocation list(Object...items) {
		HasSourceLocation tail = new Atom(S_NIL);
		for( int i=items.length; i-- > 0; ) {
			tail = new ConsPair(items[i], tail);
		}
		return tail;
	}
	static ConsPair assertConsPair(Object p, String role, HasSourceLocation sLoc) throws EvalException {
		if( !(p instanceof ConsPair) ) throw new EvalException("Expected a cons pair, but got "+p+" for "+role, sLoc);
		return (ConsPair)p;
	}
	static Object car(Object a, String role, HasSourceLocation sLoc) throws EvalException {
		return assertConsPair(a, role, sLoc).left;
	}
	static Object cdr(Object a, String role, HasSourceLocation sLoc) throws EvalException {
		return assertConsPair(a, role, sLoc).right;
	}
	static Object cadr(Object a, String role, HasSourceLocation sLoc) throws EvalException {
		return car(cdr(a, role, sLoc), role, sLoc);
	}
	
	/** Evaluate a list, returning a new list (ConsPair or Atom(S_NIL)) */
	public static HasSourceLocation evalList(HasSourceLocation listObj, Function<String, Object> defs) throws EvalException {
		if( listObj instanceof ConsPair ) {
			ConsPair pair = (ConsPair)listObj; 
			// Here's the place to add 'splat' if you want it
			return new ConsPair(
				eval((HasSourceLocation)pair.left, defs),
				evalList((HasSourceLocation)pair.right, defs),
				listObj.getSourceFileUri(),
				// We're lying; this is is the location of the expression
				// that evaluated to this new ConsPair;
				// might want to indicate that somehow!
				listObj.getSourceLineIndex(), listObj.getSourceColumnIndex(),
				listObj.getSourceEndLineIndex(), listObj.getSourceEndColumnIndex()
			);
		}
		if( isSymbol(listObj, S_NIL) ) {
			return (Atom)listObj;
		}
		throw new EvalException("Argument list is not a pair", listObj);
	}
	
	static boolean isSymbol(Object obj, String name) {
		return (obj instanceof Atom) && name.equals(((Atom)obj).text);
	}
	
	public static Object evalConsPair(ConsPair cp, Function<String,Object> defs) throws EvalException {
		HasSourceLocation funcExpr = (HasSourceLocation)cp.left;
		Object fun = eval(funcExpr, defs);
		boolean isMacro = false;
		if( fun instanceof ConsPair && isSymbol( ((ConsPair)fun).left, S_MACRO )) {
			isMacro = true;
			fun = ((ConsPair)fun).right;
		}
		if( isMacro ) {
			if( !(fun instanceof Macro) ) {
				throw new EvalException(funcExpr+" is not a macro", cp);
			}
			return ((Macro<HasSourceLocation,Object>)fun).apply((HasSourceLocation)cp.right, defs);
		} else {
			if( !(fun instanceof Function) ) {
				throw new EvalException(funcExpr+" is not a function", cp);
			}
			return ((Function<HasSourceLocation,Object>)fun).apply(evalList((HasSourceLocation)cp.right, defs));
		}
	}
	
	public static Object eval(HasSourceLocation expr, Function<String,Object> defs) throws EvalException {
		if( expr instanceof ConsPair ) {
			return evalConsPair( (ConsPair)expr, defs );
		} else if( expr instanceof Atom ) {
			String symbol = ((Atom)expr).text;
			Object val = defs.apply(symbol);
			if( val == null ) throw new EvalException("'"+symbol+"' not defined", expr);
			return val;
		} else if( expr instanceof LiteralValue ) {
			return ((LiteralValue)expr).value;
		} else {
			throw new EvalException("Don't know how to evaluate "+expr, expr);
		}
	}
	
	static class Concat implements Function<Object,Object> {
		public static Concat instance = new Concat();
		
		public Object apply(Object arg) {
			StringBuilder result = new StringBuilder();
			while( !isSymbol(arg, S_NIL) ) {
				ConsPair p = (ConsPair)arg;
				try {
					Evaluator.format(p.left, result);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				arg = p.right;
			}
			return result.toString();
		};
	};
	
	static class Cons implements Function<Object,Object> {
		public static Cons instance = new Cons();
		
		public Object apply(Object args) throws EvalException {
			return cons(
				car(args, "'cons' arg0", (HasSourceLocation)args),
				cadr(args, "'cons' arg1", (HasSourceLocation)args)
			);
		};
	};
	
	static class Head implements Function<Object,Object> {
		public static Head instance = new Head();
		
		public Object apply(Object args) throws EvalException {
			return car(car(args, "'head' argument list", (HasSourceLocation)args), "'head' argument 0", (HasSourceLocation)args);
		};
	};
	
	static class Tail implements Function<Object,Object> {
		public static Tail instance = new Tail();
		
		public Object apply(Object args) throws EvalException {
			return cdr(car(args, "'tail' argument list", (HasSourceLocation)args), "'tail' argument 0", (HasSourceLocation)args);
		};
	};
}

public class InterpreterTest extends TestCase {
	Function<String,Object> testDefs = new Function<String, Object>() {
		@Override public Object apply(String arg) {
			if( Evaluator.FN_CONCAT.equals(arg) ) {
				return Evaluator.Concat.instance;
			} else if( Evaluator.FN_CONS.equals(arg) ) {
				return Evaluator.Cons.instance;
			} else if( Evaluator.FN_HEAD.equals(arg) ) {
				return Evaluator.Head.instance;
			} else if( Evaluator.FN_TAIL.equals(arg) ) {
				return Evaluator.Tail.instance;
			} else {
				return null;
			}
		}
	};
	
	//SourceLocation sLoc = new SourceLocation("testConcatFooBar", 0, 0, 0, 0);
	
	public void testConcatFooBar() throws EvalException {
		ConsPair expression = new ConsPair(
			new Atom(Evaluator.FN_CONCAT),
			new ConsPair(
				new LiteralValue("foo"),
				new ConsPair(
					new LiteralValue("bar"),
					new Atom(Evaluator.S_NIL))));
		assertEquals("foobar", Evaluator.eval(expression, testDefs));
	}
	
	public void testConsFooBar() throws EvalException {
		ConsPair expression = new ConsPair(
			new Atom(Evaluator.FN_CONS),
			new ConsPair(
				new LiteralValue("foo"),
				new ConsPair(
					new LiteralValue("bar"),
					new Atom(Evaluator.S_NIL))));
		assertEquals(new ConsPair("foo", "bar"), Evaluator.eval(expression, testDefs));
	}
	
	public void testHeadConsFooBar() throws EvalException {
		HasSourceLocation expression = Evaluator.list(
			new Atom(Evaluator.FN_HEAD),
			Evaluator.list(
				new Atom(Evaluator.FN_CONS),
				new LiteralValue("foo"),
				new LiteralValue("bar")
			)
		);
		assertEquals("foo", Evaluator.eval(expression, testDefs));
	}
	
	public void testTailConsFooBar() throws EvalException {
		HasSourceLocation expression = Evaluator.list(
			new Atom(Evaluator.FN_TAIL),
			Evaluator.list(
				new Atom(Evaluator.FN_CONS),
				new LiteralValue("foo"),
				new LiteralValue("bar")
			)
		);
		assertEquals("bar", Evaluator.eval(expression, testDefs));
	}
	
	public void testEvalUndefinedSymbolThrows() {
		EvalException caught = null;
		try {
			Evaluator.eval(new Atom("dne"), testDefs);
		} catch( EvalException e ) {
			caught = e;
		};
		assertNotNull(caught);
	}
}
