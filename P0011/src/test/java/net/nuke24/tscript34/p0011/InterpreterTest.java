package net.nuke24.tscript34.p0011;

import java.io.IOException;

import javax.script.ScriptException;

import junit.framework.TestCase;
import net.nuke24.tscript34.p0011.sexp.AbstractExpression;
import net.nuke24.tscript34.p0011.sexp.Atom;
import net.nuke24.tscript34.p0011.sexp.ConsPair;
import net.nuke24.tscript34.p0011.sexp.LiteralValue;

class SourceLocation implements HasSourceLocation {
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
	@Override public String getSourceFileUri() { return sourceUri; }
	@Override public int getSourceLineIndex() { return sourceLineIndex; }
	@Override public int getSourceColumnIndex() { return sourceColumnIndex; }
	@Override public int getSourceEndLineIndex() { return sourceEndLineIndex; }
	@Override public int getSourceEndColumnIndex() { return sourceEndColumnIndex; }
}

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
	
	static String S_NIL = "http://ns.nuke24.net/TScript34/P0011/Values/Nil";
	static String S_MACRO = "http://ns.nuke24.net/TScript34/P0011/X/Macro";
	static String S_QUOTE = "http://ns.nuke24.net/TScript34/P0011/Macro/Quote";
	
	static void format(Object obj, Appendable dest) throws IOException {
		dest.append(obj.toString());
	}
	
	/** Evaluate a list, returning a new list (ConsPair or Atom(S_NIL)) */
	public static AbstractExpression evalList(HasSourceLocation listObj, Function<String, Object> defs) throws EvalException {
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
		AbstractExpression funcExpr = (AbstractExpression)cp.left;
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
			return ((Macro<AbstractExpression,Object>)fun).apply((AbstractExpression)cp.right, defs);
		} else {
			if( !(fun instanceof Function) ) {
				throw new EvalException(funcExpr+" is not a function", cp);
			}
			return ((Function<AbstractExpression,Object>)fun).apply(evalList((AbstractExpression)cp.right, defs));
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
	
	static class Concat implements Function<AbstractExpression,Object> {
		public static Concat instance = new Concat();
		
		public Object apply(AbstractExpression arg) {
			StringBuilder result = new StringBuilder();
			while( !isSymbol(arg, S_NIL) ) {
				ConsPair p = (ConsPair)arg;
				try {
					Evaluator.format(p.left, result);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				arg = (AbstractExpression)p.right;
			}
			return result.toString();
		};
	};
}

public class InterpreterTest extends TestCase {
	Function<String,Object> testDefs = new Function<String, Object>() {
		@Override public Object apply(String arg) {
			if( "concat".equals(arg) ) {
				return Evaluator.Concat.instance;
			} else {
				return null;
			}
		}
	};
	
	//SourceLocation sLoc = new SourceLocation("testConcatFooBar", 0, 0, 0, 0);
	
	public void testConcatFooBar() throws EvalException {
		ConsPair expression = new ConsPair(
			new Atom("concat"),
			new ConsPair(
				new LiteralValue("foo"),
				new ConsPair(
					new LiteralValue("bar"),
					new Atom(Evaluator.S_NIL))));
		assertEquals("foobar", Evaluator.eval(expression, testDefs));
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
