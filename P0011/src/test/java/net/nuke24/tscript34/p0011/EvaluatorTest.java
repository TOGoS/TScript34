package net.nuke24.tscript34.p0011;

import java.io.IOException;

import javax.script.ScriptException;

import junit.framework.TestCase;

import net.nuke24.tscript34.p0011.sexp.Atom;
import net.nuke24.tscript34.p0011.sexp.ConsPair;
import net.nuke24.tscript34.p0011.sexp.LiteralValue;
import net.nuke24.tscript34.p0011.sexp.Symbols;
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
	
	static void format(Object obj, Appendable dest) throws IOException {
		dest.append(obj.toString());
	}
	
	static ConsPair cons(Object a, Object b) {
		return new ConsPair(a, b);
	}
	static HasSourceLocation list(Object...items) {
		HasSourceLocation tail = Symbols.NIL;
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
		if( isNil(listObj) ) return (Atom)listObj;
		throw new EvalException("Argument list is not a pair", listObj);
	}
	
	static boolean isSymbol(Object obj, String name) {
		return (obj instanceof Atom) && name.equals(((Atom)obj).text);
	}
	
	static boolean isNil(Object obj) {
		return isSymbol(obj, Symbols.S_NIL);
	}
	
	public static Object evalConsPair(ConsPair cp, Function<String,Object> defs) throws EvalException {
		HasSourceLocation funcExpr = (HasSourceLocation)cp.left;
		Object fun = eval(funcExpr, defs);
		boolean isMacro = false;
		if( fun instanceof ConsPair && isSymbol( ((ConsPair)fun).left, Symbols.S_MACRO )) {
			// I must have earlier been thinking that relying on 'marker interface' alone
			// is bad for some reason, so macros should be marked instead
			// by being represented as (macro-marker macro-implementation).
			// That might be silly.
			isMacro = true;
			fun = ((ConsPair)fun).right;
		} else if( fun instanceof Macro ) {
			// A more straightforward rpresentation:
			isMacro = true;
		}
		if( isMacro ) {
			if( !(fun instanceof Macro) ) {
				throw new EvalException(funcExpr+" is not a macro", cp);
			}
			@SuppressWarnings("unchecked")
			Macro<HasSourceLocation,Object> mac = (Macro<HasSourceLocation, Object>)fun;
			return mac.apply((HasSourceLocation)cp.right, defs);
		} else {
			if( !(fun instanceof Function) ) {
				throw new EvalException(funcExpr+" is not a function", cp);
			}
			@SuppressWarnings("unchecked")
			Function<HasSourceLocation,Object> func = (Function<HasSourceLocation,Object>)fun;
			return func.apply(evalList((HasSourceLocation)cp.right, defs));
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
			while( !isSymbol(arg, Symbols.S_NIL) ) {
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
	
	static class MakeLambda implements Macro<Object,Object> {
		public static final MakeLambda instance = new MakeLambda();
		
		static Function<String,Object> bindParams(Object params, Object values, Function<String,Object> parent) throws EvalException {
			while( !isNil(params) ) {
				Object param = car(params, "argument name", (HasSourceLocation)params);
				// May allow more complex parameter definitions later,
				// but for now, assume they are all symbols.
				final String name = ((Atom)param).text;
				if( isNil(values) ) {
					throw new RuntimeException("Unexpected end of argument list at "+name);
				}
				final Object value = car(values, "value for parameter '"+name+"'", (HasSourceLocation)values);
				final Function<String,Object> _p = parent;
				parent = new Function<String,Object>() {
					@Override
					public Object apply(String arg) throws EvalException {
						return (name.equals(arg)) ? value : _p.apply(arg);
					}
				};
				params = cdr(params, "rest of parameter list", (HasSourceLocation)params);
				values = cdr(values, "rest of argument list", (HasSourceLocation)values);
			}
			return parent;
		}
		
		@Override
		public Object apply(Object arg, final Function<String, Object> defs) throws EvalException {
			HasSourceLocation sLoc = (HasSourceLocation)arg;
			Object params = car(arg, "lambda argument list", sLoc);
			HasSourceLocation body = (HasSourceLocation)cadr(arg, "lambda body", sLoc);
			return new Function<Object,Object>() {
				@Override
				public Object apply(Object arg) throws EvalException {
					Function<String,Object> innerDefs = bindParams(params, arg, defs);
					return eval(body, innerDefs);
				}
			};
		}
	}
}

public class EvaluatorTest extends TestCase {
	Function<String,Object> testDefs = new Function<String, Object>() {
		@Override public Object apply(String arg) {
			if( Symbols.FN_CONCAT.equals(arg) ) {
				return Evaluator.Concat.instance;
			} else if( Symbols.FN_CONS.equals(arg) ) {
				return Evaluator.Cons.instance;
			} else if( Symbols.FN_HEAD.equals(arg) ) {
				return Evaluator.Head.instance;
			} else if( Symbols.FN_TAIL.equals(arg) ) {
				return Evaluator.Tail.instance;
			} else if( Symbols.MN_LAMBDA.equals(arg) ) {
				return Evaluator.MakeLambda.instance;
			} else {
				return null;
			}
		}
	};
	
	//SourceLocation sLoc = new SourceLocation("testConcatFooBar", 0, 0, 0, 0);
	
	public void testConcatFooBar() throws EvalException {
		ConsPair expression = new ConsPair(
			new Atom(Symbols.FN_CONCAT),
			new ConsPair(
				new LiteralValue("foo"),
				new ConsPair(
					new LiteralValue("bar"),
					new Atom(Symbols.S_NIL))));
		assertEquals("foobar", Evaluator.eval(expression, testDefs));
	}
	
	public void testConsFooBar() throws EvalException {
		ConsPair expression = new ConsPair(
			new Atom(Symbols.FN_CONS),
			new ConsPair(
				new LiteralValue("foo"),
				new ConsPair(
					new LiteralValue("bar"),
					new Atom(Symbols.S_NIL))));
		assertEquals(new ConsPair("foo", "bar"), Evaluator.eval(expression, testDefs));
	}
	
	public void testHeadConsFooBar() throws EvalException {
		HasSourceLocation expression = Evaluator.list(
			new Atom(Symbols.FN_HEAD),
			Evaluator.list(
				new Atom(Symbols.FN_CONS),
				new LiteralValue("foo"),
				new LiteralValue("bar")
			)
		);
		assertEquals("foo", Evaluator.eval(expression, testDefs));
	}
	
	public void testTailConsFooBar() throws EvalException {
		HasSourceLocation expression = Evaluator.list(
			new Atom(Symbols.FN_TAIL),
			Evaluator.list(
				new Atom(Symbols.FN_CONS),
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
	
	public void testLambda() throws EvalException {
		Object lambda = Evaluator.eval(Evaluator.list(
			new Atom(Symbols.MN_LAMBDA),
			Evaluator.list(
				new Atom("left"),
				new Atom("right")
			),
			Evaluator.list(
				new Atom(Symbols.FN_CONCAT),
				new Atom("left"),
				new LiteralValue(" and "),
				new Atom("right")
			)
		), testDefs);
		Object result = ((Function)lambda).apply(Evaluator.list("foo","bar"));
		assertEquals("foo and bar", result);
	}
}
