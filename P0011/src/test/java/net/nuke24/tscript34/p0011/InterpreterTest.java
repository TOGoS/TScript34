package net.nuke24.tscript34.p0011;

import static org.junit.Assert.assertThrows;

import java.io.IOException;

import javax.script.ScriptException;

import junit.framework.TestCase;

class SourceLocation {
	public String sourceUri;
	public final int sourceLineIndex;
	public final int sourceColumnIndex;
	public final int sourceEndLineIndex;
	public final int sourceEndColumnIndex;
	public SourceLocation(String sourceUri, int sourceLineIndex, int sourceColumnIndex, int sourceEndLineIndex, int sourceEndColumnIndex) {
		this.sourceUri = sourceUri;
		this.sourceLineIndex = sourceLineIndex;
		this.sourceColumnIndex = sourceColumnIndex;
		this.sourceEndLineIndex = sourceEndColumnIndex;
		this.sourceEndColumnIndex = sourceEndColumnIndex;
	}
}

class EvalException extends ScriptException {
	private static final long serialVersionUID = 3707037127440615004L;
	final SourceLocation sLoc;
	public EvalException(String message, SourceLocation sLoc) {
		super(message, sLoc.sourceUri, sLoc.sourceLineIndex+1, sLoc.sourceColumnIndex+1);
		this.sLoc = sLoc;
	}
}

interface Function<T,R> {
	public R apply(T arg) throws EvalException;
}

class Evaluator {
	// Interpreter values:
	// (cons a b) = Pair(a, b)
	// () = null
	// foo = Symbol("foo")
	
	interface Expression {
		Object eval(Function<String,Object> defs) throws EvalException;
	}
	static class Nil implements Expression {
		public static Nil instance = new Nil();
		private Nil() { }
		@Override
		public Object eval(Function<String, Object> defs) {
			return this;
		}
	}
	static class Pair implements Expression {
		public final Object left;
		public final Object right;
		public final SourceLocation sLoc;
		public Pair(Object left, Object right, SourceLocation sLoc) {
			this.left = left;
			this.right = right;
			this.sLoc = sLoc;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public Object eval(Function<String, Object> defs) throws EvalException {
			Object fun = Evaluator.eval(this.left, defs);
			if( !(fun instanceof Function) ) {
				throw new EvalException(fun+" is not a function", this.sLoc);
			}
			return ((Function<Object,Object>)fun).apply(evalList(this.right, defs, this.sLoc));
		}
	}
	static class Literal implements Expression {
		Object value;
		Literal(Object value) {
			this.value = value;
		}
		@Override
		public Object eval(Function<String, Object> defs) {
			return value;
		}
	}
	static class Symbol implements Expression {
		public final String name;
		public final SourceLocation sLoc;
		public Symbol(String name, SourceLocation sLoc) {
			this.name = name;
			this.sLoc = sLoc;
		}
		@Override public Object eval(Function<String, Object> defs) throws EvalException {
			Object result = defs.apply(this.name);
			if( result == null ) throw new EvalException(name, sLoc);
			return result;
		}
	}
	
	static void format(Object obj, Appendable dest) throws IOException {
		dest.append(obj.toString());
	}

	public static Object evalList(Object listObj, Function<String, Object> defs, SourceLocation sLoc) throws EvalException {
		if( listObj == Nil.instance ) return Nil.instance;
		if( listObj instanceof Pair ) {
			Pair pair = (Pair)listObj; 
			// Here's the place to add 'splat' if you want it
			return new Pair(Evaluator.eval(pair.left, defs), evalList(pair.right, defs, pair.sLoc), pair.sLoc);
		}
		throw new EvalException("Argument list is not a pair", sLoc);
	}
	
	public static Object eval(Object obj, Function<String,Object> defs) throws EvalException {
		if( obj instanceof Expression ) {
			return ((Expression)obj).eval(defs);
		} else {
			return obj;
		}
	}
	
	static class Concat implements Function<Object,Object> {
		public static Concat instance = new Concat();
		
		public Object apply(Object arg) {
			StringBuilder result = new StringBuilder();
			while( arg != Evaluator.Nil.instance ) {
				Evaluator.Pair p = (Evaluator.Pair)arg;
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
}

public class InterpreterTest extends TestCase {
	Function<String,Object> testDefs = new Function<String, Object>() {
		@Override
		public Object apply(String arg) {
			if( "concat".equals(arg) ) {
				return Evaluator.Concat.instance;
			} else {
				return null;
			}
		}
	};
	
	SourceLocation sLoc = new SourceLocation("testConcatFooBar", 0, 0, 0, 0);
	
	public void testConcatFooBar() throws EvalException {
		Evaluator.Pair program = new Evaluator.Pair(new Evaluator.Symbol("concat", sLoc), new Evaluator.Pair("foo", new Evaluator.Pair("bar", Evaluator.Nil.instance, sLoc), sLoc), sLoc);
		assertEquals("foobar", program.eval(testDefs));
	}
	
	public void testEvalUndefinedSymbolThrows() {
		EvalException caught = null;
		try {
			new Evaluator.Symbol("dne", sLoc).eval(testDefs);
		} catch( EvalException e ) {
			caught = e;
		};
		assertNotNull(caught);
	}
}
