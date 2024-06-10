package net.nuke24.tscript34.p0019;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.nuke24.tscript34.p0019.effect.Absorb;
import net.nuke24.tscript34.p0019.effect.Emit;
import net.nuke24.tscript34.p0019.effect.EndOfProgramReached;
import net.nuke24.tscript34.p0019.effect.QuitWithCode;
import net.nuke24.tscript34.p0019.effect.ResumeWith;
import net.nuke24.tscript34.p0019.effect.ReturnWithValue;
import net.nuke24.tscript34.p0019.effect.StackUnderflowException;
import net.nuke24.tscript34.p0019.iface.Consumer;
import net.nuke24.tscript34.p0019.iface.InterpreterState;
import net.nuke24.tscript34.p0019.util.Charsets;
import net.nuke24.tscript34.p0019.util.DebugFormat;
import net.nuke24.tscript34.p0019.value.Concatenation;
import net.nuke24.tscript34.p0019.value.Symbol;

public class P0019 {
	public static final Symbol MARK = new Symbol("http://ns.nuke24.net/TScript34/P0019/Constants/Mark");
	
	public static String NAME = "TS34.19";
	public static String VERSION = "0.0.3";
	
	public static int EXITCODE_NORMAL = 0;
	// Program tried to read from input, but input's closed
	public static int EXITCODE_READ_END = -15;
	public static int EXITCODE_EXCEPTION = 1;
	
	public static String OP_OPEN_PROC = "http://ns.nuke24.net/TScript34/Ops/OpenProcedure";
	public static String OP_CLOSE_PROC = "http://ns.nuke24.net/TScript34/Ops/CloseProcedure";
	
	public static String OPC_ALIAS = "http://ns.nuke24.net/TScript34/Op/Alias";
	public static String OPC_PUSH_VALUE = "http://ns.nuke24.net/TScript34/Op/PushValue";
	public static String OPC_PUSH_SYMBOL = "http://ns.nuke24.net/TScript34/Op/PushSymbol";
	
	// <name>:<op constructor arg count>:<pop count>:<push count>
	public static String OP_ARRAY_FROM_STACK = "http://ns.nuke24.net/TScript34/Ops/ArrayFromStack";
	public static String OP_CONCAT_N = "http://ns.nuke24.net/TScript34/P0009/Ops/ConcatN"; // item0 item1 ... itemN n -- concatenated
	public static String OP_COUNT_TO_MARK = "http://ns.nuke24.net/TScript34/Ops/CountToMark";
	public static String OP_DROP = "http://ns.nuke24.net/TScript34/Ops/Drop";
	public static String OP_DUP = "http://ns.nuke24.net/TScript34/Ops/Dup";
	public static String OP_EXCH = "http://ns.nuke24.net/TScript34/Ops/Exch";
	public static String OP_EXECUTE = "http://ns.nuke24.net/TScript34/Ops/Exec";
	public static String OP_GET_PROPERTY_VALUES = "http://ns.nuke24.net/TScript34/Ops/GetPropertyValues"; 
	public static String OP_GET_INTERPRETER_INFO = "http://ns.nuke24.net/TScript34/Ops/GetInterpreterInfo";
	public static String OP_JUMP = "http://ns.nuke24.net/TScript34/P0009/Ops/Jump";
	public static String OP_POP = "http://ns.nuke24.net/TScript34/Ops/Pop";
	public static String OP_PUSH_MARK = "http://ns.nuke24.net/TScript34/Ops/PushMark";
	public static String OP_PRINT = "http://ns.nuke24.net/TScript34/Ops/Print";
	public static String OP_PRINT_LINE = "http://ns.nuke24.net/TScript34/Ops/PrintLine";
	public static String OP_PRINT_STACK_THUNKS = "http://ns.nuke24.net/TScript34/Ops/PrintStackThunks";
	public static String OP_RDF_OBJECT_FROM_STACK = "http://ns.nuke24.net/TScript34/Ops/RDFObjectFromStack"; 
	public static String OP_QUIT = "http://ns.nuke24.net/TScript34/Ops/Quit";
	public static String OP_QUIT_WITH_CODE = "http://ns.nuke24.net/TScript34/Ops/QuitWithCode";
	public static String OP_RETURN = "http://ns.nuke24.net/TScript34/Ops/Return";
	
	public static String DATATYPE_DECIMAL = "http://www.w3.org/2001/XMLSchema#decimal";
	
	protected static final Pattern dataUriPat = Pattern.compile("^data:,(.*)");
	protected static final Pattern decimalIntegerPat = Pattern.compile("^(\\d+)$");
	protected static final Pattern hexIntegerPat = Pattern.compile("^0x([\\da-fA-F]+)$");
	
	public static Object decodeUri(String uri) {
		Matcher m;
		if( (m = dataUriPat.matcher(uri)).matches() ) {
			try {
				return URLDecoder.decode(m.group(1), "UTF-8");
			} catch( UnsupportedEncodingException e ) {
				throw new RuntimeException("Failed to decode data URI", e);
			}
		} else {
			throw new RuntimeException("Unrecognized URI '"+uri+"'");
		}
	}
	
	public static Object decodeTs34(String[] tokens, int idx) {
		if( tokens.length <= idx ) {
			throw new RuntimeException("decodeTs34 requires at least one token");
		}
		Object decoded = decodeUri(tokens[idx++]);
		while( idx < tokens.length ) {
			String encodingName = tokens[idx];
			if( DATATYPE_DECIMAL.equals(encodingName) ) {
				decoded = Integer.valueOf(decoded.toString());
			} else {
				// Or could build TS34Encoded object or such
				throw new RuntimeException("Unrecognized encoding: "+encodingName);
			}
			++idx;
		}
		return decoded;
	}
	
	public static void append(Object obj, Appendable dest) throws IOException {
		if( obj instanceof Concatenation<?> ) {
			Concatenation<?> cat = (Concatenation<?>)obj;
			for( Object elem : cat.children ) {
				append(elem, dest);
			}
		} else {
			dest.append(toString(obj));
		}
	}
	public static String toString(Object obj) {
		if( obj instanceof Concatenation<?> ) {
			StringBuilder sb = new StringBuilder();
			try {
				append(obj, sb);
			} catch( IOException e ) {
				throw new RuntimeException("Unexpected IOException when appending to StringBuilder", e);
			}
			return sb.toString();
		} else {
			return DebugFormat.toDebugString(obj)+" (debug string)";
		}
	}
	
	public static int toInt(Object obj) {
		if( obj instanceof Number ) {
			return ((Number)obj).intValue();
		} else {
			throw new RuntimeException("Not a number: "+obj);
		}
	}
	
	public static void writeTo(Object obj, OutputStream os) throws IOException {
		if( obj == null ) {
			System.err.println("Warning: null passed to writeTo; decide whether this is allowed");
		} else if( obj instanceof byte[] ) {
			os.write((byte[])obj);
		} else if( obj instanceof Concatenation<?> ) {
			Concatenation<?> cat = ((Concatenation<?>)obj);
			for( Object elem : cat.children ) writeTo(elem, os);
		} else if( obj instanceof CharSequence ) {
			os.write(((CharSequence)obj).toString().getBytes(Charsets.UTF8));
		} else {
			if( os instanceof Appendable ) {
				append(obj, (Appendable)os );
			} else {
				os.write(toString(obj).getBytes(Charsets.UTF8));
			}
		}
	}
	
	public static InputStream getInputStream(String pathOrUri) throws IOException {
		// TODO: Check for URIs, being careful about those one-letter
		// URI schemes that are actually Windows file paths.
		// (JCR36 already does this; maybe shove that decoding into a library)
		return new FileInputStream(pathOrUri);
	}
	
	//// Stack-based interpreter and ops
	
	/**
	 * State of an interpreter that only returns a value
	 * and can't be advanced.
	 */
	static class ReturnInterpreterState<A, E> implements InterpreterState<A, E> {
		final E request;
		public ReturnInterpreterState(E request) {
			this.request = request;
		}
		@Override public E getRequest() { return this.request; }
		@Override public InterpreterState<? super A, ? extends E> advance(A arg, int maxSteps) {
			throw new RuntimeException("Shouldn't've been called!");
		}
	}
	
	// TODO: For this system to work at all
	// the stack elements and effects are all of type Object.
	// Maybe just remove the type parameters.  :P
	interface StackyBlockOp<A,E> {
		/**
		 * Instructions can directly read and modify the stack.
		 * Anything else requires returning an effect.
		 * Return an effect to request an effect and have
		 * the return value (if the effect calls for one) pushed onto the stack.
		 * Return null to not request an effect.
		 */
		public E execute(List<A> stack);
	}
	static class BeginBlockOp<V> implements StackyBlockOp<V,Object> {
		public final EndBlockOp<Object> instance = new EndBlockOp<Object>();
		private BeginBlockOp() { }
		@Override public Object execute(List<V> stack) {
			return new RuntimeException("Begin block op should not be executed");
		}
	}
	static class DropOp<V> implements StackyBlockOp<V,Object> {
		public static final DropOp<Object> instance = new DropOp<Object>();
		private DropOp() { }
		@Override public Object execute(List<V> stack) {
			if( stack.size() < 1 ) {
				return new StackUnderflowException("Stack underflow; Drop requires one item");
			}
			stack.remove(stack.size()-1);
			return null;
		}
	}
	static class DupOp<V> implements StackyBlockOp<V,Object> {
		public static final DupOp<Object> instance = new DupOp<Object>();
		private DupOp() { }
		@Override public Object execute(List<V> stack) {
			if( stack.size() < 1 ) {
				return new StackUnderflowException("Stack underflow; Dup requires one item");
			}
			stack.add(stack.get(stack.size()-1));
			return null;
		}
	}
	static class ExchOp<V> implements StackyBlockOp<V,Object> {
		public static final ExchOp<Object> instance = new ExchOp<Object>();
		private ExchOp() { }
		@Override public Object execute(List<V> stack) {
			if( stack.size() < 2 ) {
				return new StackUnderflowException("Stack underflow; Exch requires two items");
			}
			V a = stack.get(stack.size()-1);
			stack.set(stack.size()-1, stack.get(stack.size()-2));
			stack.set(stack.size()-1, a);
			return null;
		}
	}
	static class EndBlockOp<V> implements StackyBlockOp<V,Object> {
		public final EndBlockOp<Object> instance = new EndBlockOp<Object>();
		private EndBlockOp() { }
		public Object execute(List<V> stack) {
			return new RuntimeException("End block op should not be executed");
		}
	}
	/** -- value */
	static class PushOp<V> implements StackyBlockOp<V,Object> {
		public static <V> PushOp<V> of(V value) {
			return new PushOp<V>(value);
		}
		final V value;
		protected PushOp(V value) {
			this.value = value;
		}
		@Override public Object execute(List<V> stack) {
			stack.add(value);
			return null;
		}
	}
	/** thing --{ emit thing }-- */
	static class PopAndEmitOp<V> implements StackyBlockOp<V,Object> {
		public static final PopAndEmitOp<Object> instance = new PopAndEmitOp<Object>();
		private PopAndEmitOp() {}
		@Override public Object execute(List<V> stack) {
			if( stack.size() < 1 ) {
				return new StackUnderflowException("Emit requires one item on the stack");
			}
			Object v = stack.remove(stack.size()-1);
			return new Emit<Object, Object>(null, v);
		}
	}
	static class EffectOp<V,E> implements StackyBlockOp<V,E> {
		final E effect;
		public EffectOp(E effect) {
			this.effect = effect;
		}
		@Override public E execute(List<V> stack) {
			return effect;
		}
	}
	// Pops a return continuation and list of ops from the stack and jumps to it!
	// (op-list return-continuation --)  
	static class PopAndJumpOp<V> implements StackyBlockOp<V,Object> {
		public static final PopAndJumpOp<Object> instance = new PopAndJumpOp<Object>();
		private PopAndJumpOp() {}
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override public Object execute(List<V> stack) {
			Object contObj = stack.remove(stack.size()-1);
			Object opsObj = stack.remove(stack.size()-1);
			try {
				Continuation<StackyBlockOp<V,Object>> cont = (Continuation<StackyBlockOp<V,Object>>)contObj;
				List<?> ops = (List<?>)opsObj;
				return new Continuation(ops, 0, cont);
			} catch( ClassCastException e ) {
				// TODO: Maybe wrap in a ScriptException or something
				return e;
			}
		}
	}
	static class ReturnOp<V> implements StackyBlockOp<V,Object> {
		public static final ReturnOp<Object> instance = new ReturnOp<Object>();
		private ReturnOp() {}
		@Override public Object execute(List<V> stack) {
			return new ReturnWithValue<Object>(Collections.unmodifiableList(stack));
		}
	}
	static class PopAndQuitWithCodeOp<V> implements StackyBlockOp<V,Object> {
		public static final PopAndQuitWithCodeOp<Object> instance = new PopAndQuitWithCodeOp<Object>();
		private PopAndQuitWithCodeOp() {}
		@Override public Object execute(List<V> stack) {
			Object v = stack.remove(stack.size()-1);
			return new QuitWithCode(toInt(v));
		}
	}
	// (sequence item -- sequence+item)
	static class AppendOp implements StackyBlockOp<Object,Object> {
		public static final AppendOp instance = new AppendOp();
		@Override
		public Object execute(List<Object> stack) {
			Object item = stack.remove(stack.size()-1);
			List<Object> list = (List<Object>)stack.remove(stack.size()-1);
			List<Object> newList = new ArrayList<Object>(list);
			newList.add(item);
			stack.add(newList);
			return null;
		}
	}
	static class ConcatNOp implements StackyBlockOp<Object,Object> {
		public static final ConcatNOp instance = new ConcatNOp();
		@Override public Object execute(List<Object> stack) {
			int n = toInt(stack.remove(stack.size()-1));
			Object[] children = new Object[n];
			for( int i=0; i<n; ++i ) {
				children[i] = stack.get(stack.size()-n+i);
			}
			for( int i=0; i<n; ++i ) {
				stack.remove(stack.size()-1);
			}
			stack.add(new Concatenation<Object>(children));
			return null;
		}
	}
	static class CountToOp implements StackyBlockOp<Object,Object> {
		public static final CountToOp countToMark = new CountToOp(MARK);
		protected final Object mark;
		public CountToOp(Object mark) {
			this.mark = mark;
		}
		@Override public Object execute(List<Object> stack) {
			for( int i=stack.size(); i-- > 0; ) {
				if( stack.get(i) == mark ) {
					stack.add(stack.size()-i-1);
					return null;
				}
			}
			return new RuntimeException(mark+" not found in stack");
		}
	}
	
	static final Map<String,StackyBlockOp<Object,Object>> STANDARD_OPS = new HashMap<String,StackyBlockOp<Object,Object>>();
	static {
		STANDARD_OPS.put(OP_DROP, DropOp.instance);
		STANDARD_OPS.put(OP_DUP ,  DupOp.instance);
		STANDARD_OPS.put(OP_EXCH, ExchOp.instance);
	}
	
	static final int IP_EXIT_INTERP_LOOP = -10;
	
	static final class Continuation<I> {
		private static final Continuation<Object> EXIT_INTERP_LOOP = new Continuation<Object>(Collections.emptyList(), IP_EXIT_INTERP_LOOP);
		
		public final List<I> block;
		public final int index;
		// Why just one returnTo?
		// Maybe we should allow multiple ways to return!
		// (without involving the data stack)
		// e.g. might want separate destinations for
		// explicit return vs fall-through when at top-level
		// (return quitting, falling through resuming the read-execute loop)
		public final Continuation<I> returnTo;
		private Continuation(List<I> block, int index) {
			this.block = block;
			this.index = index;
			this.returnTo = this;
		}
		private Continuation(List<I> block, int index, Continuation<I> returnTo) {
			if( returnTo == null ) {
				throw new RuntimeException("Bad returnTo!");
			}
			this.block = block;
			this.index = index;
			this.returnTo = returnTo;
		}
		
		@SuppressWarnings("unchecked")
		public static <I> Continuation<I> exitInterpLoop() {
			return (Continuation<I>) EXIT_INTERP_LOOP;
		}
		
		public static <I> Continuation<I> to(List<I> block, int index, Continuation<I> returnTo) {
			assert returnTo != null;
			return new Continuation<I>(block, index, returnTo);
		}
		
		@Override public String toString() {
			return "Continuation["+block.size()+" instructions, index="+index+", returnTo="+(returnTo == this ? "self" : returnTo)+"]";
		}
	}
	
	static class StackyBlockInterpreterState<A,E> implements InterpreterState<A, E> {
		public static final Object PushCurrentReturnContinuation = new Object();
		
		// Might replace this with a stack of them, i.e. the return stack
		final E request;
		final Continuation<StackyBlockOp<A,E>> next;
		final List<A> dataStack;
		
		public StackyBlockInterpreterState(
			E request,
			Continuation<StackyBlockOp<A,E>> next,
			List<A> dataStack
		) {
			this.request = request;
			this.next = next;
			this.dataStack = dataStack;
		}
		@Override public E getRequest() { return request; }
		@SuppressWarnings("unchecked")
		@Override
		public InterpreterState<A,E> advance(A arg, int maxSteps) {
			List<StackyBlockOp<A,E>> instructions = next.block;
			List<A> stack = new ArrayList<A>(dataStack);
			Continuation<StackyBlockOp<A,E>> returnTo = next.returnTo;
			if( arg != null ) {
				stack.add(arg);
			}
			int ip = next.index;
			E request = null;
			while( request == null && ip < instructions.size() && maxSteps-- > 0 ) {
				if( ip == IP_EXIT_INTERP_LOOP ) {
					// By convention, the top value on the stack is 'the return value'.
					// Since we're exiting stack-land, reflect it by putting
					// it into the Return effect:
					// (This is a somewhat arbitrary choice, and maybe the
					// stack -> return value functon should be configurable,
					// or maybe the return value should just be the entire stack!
					// ...which might actually make more sense and
					// give continuation handling better symmetry.)
					return new ReturnInterpreterState<A,E>((E)new ReturnWithValue<List<A>>(Collections.unmodifiableList(stack)));
				}
				if( ip < 0 ) {
					throw new RuntimeException("Bad instruction pointer: "+ip);
				}
				request = instructions.get(ip++).execute(stack);
				if( request == PushCurrentReturnContinuation ) {
					stack.add((A)returnTo);
					request = null;
				} else if( request instanceof ReturnWithValue<?> ) {
					ReturnWithValue<List<A>> ret = (ReturnWithValue<List<A>>)request;
					if( ret.value == null || !(ret.value instanceof List<?>) ) {
						throw new RuntimeException("ReturnWithValue value should be a whole stack, a List<Object>; got "+DebugFormat.toDebugString(ret.value));
					}
					stack = ret.value;
					request = (E) returnTo;
				}
				// Continuation = JumpTo(Continuation);
				// I just was lazy and didn't want to add a new object.
				if( request instanceof Continuation ) {
					Continuation<StackyBlockOp<A,E>> continuation = (Continuation<StackyBlockOp<A,E>>)request;
					instructions = continuation.block;
					ip = continuation.index;
					returnTo = continuation.returnTo;
					request = null;
				}
			}
			Continuation<StackyBlockOp<A,E>> continuation;
			if( request == null ) {
				assert ip == instructions.size();
				continuation = returnTo;
				System.err.println("Reached end of program at ip = "+ip);
				// Reached to end of program!
				// Let's say for now that this is an error.
				// If you want to return, add a return op.
				request = (E)new EndOfProgramReached();
				// actually for now, let reaching end of program
				// is an implicit return, to make the read-execute loop
				// work even though it's not explicitly adding
				// return ops to the end of op lists
				// (since it doesn't know if it's immediately executing
				// or making a procedure, asjndkajsndkaj)
			} else {
				continuation = new Continuation<StackyBlockOp<A, E>>(instructions, ip, returnTo);
			}
			return new StackyBlockInterpreterState<A,E>(
				request,
				continuation,
				Collections.unmodifiableList(stack)
			);
		}
	}
	
	@SuppressWarnings("unchecked")
	static List<StackyBlockOp<Object,Object>> readAndJumpOps(
		Object readOpsRequest,
		Continuation<StackyBlockOp<Object,Object>> andThen,
		Continuation<StackyBlockOp<Object,Object>> onEof
	) {
		final ArrayList<StackyBlockOp<Object,Object>> rjOps = new ArrayList<StackyBlockOp<Object,Object>>();
		rjOps.add(new EffectOp<Object,Object>(readOpsRequest));
		rjOps.add(new StackyBlockOp<Object,Object>() {
			@Override public Object execute(List<Object> stack) {
				List<Object> opsRead = (List<Object>)stack.get(stack.size()-1);
				if( opsRead.size() == 0 ) {
					stack.remove(stack.size()-1);
					return onEof;
				}
				return null;
			}
		});
		// newOps
		rjOps.add(new PushOp<Object>(
			new EffectOp<Object,Object>(andThen)
		));
		// newOps jumpToContinuatoin 
		rjOps.add(AppendOp.instance);
		// newOps+jumpToContinuation
		rjOps.add(new EffectOp<Object,Object>(StackyBlockInterpreterState.PushCurrentReturnContinuation));
		// newOps+jumpToContinuation returnTo
		rjOps.add((StackyBlockOp<Object,Object>)PopAndJumpOp.instance);
		return rjOps;
	}
	
	interface SimpleExecutable<R> {
		R execute();
	}
	
	/** Exercse the InterpreterState API in a minimal way
	 * 
	 * Passing the emitter as the 'context'
	 * for now.  Maybe it should just be built-in like
	 * the other thingies, or maybe they should 
	 * */
	static class BabbyInterpreterHarness implements SimpleExecutable<Integer> {
		public static final Object readOpsRequest = new Object();
		
		// For now there's just one input channel,
		// and it's from where the script is read
		// TODO: Pass a separate ops reader
		private final boolean interactive;
		// TODO: Replace lineReader with an op reader
		private final BufferedReader lineReader;
		private InterpreterState<Object, ?> interpState;
		private final Consumer<Object> emitter;
		public BabbyInterpreterHarness(
			boolean interactive,
			BufferedReader lineReader,
			InterpreterState<Object,?> interpState,
			Consumer<Object> emitter
		) {
			this.interactive = interactive;
			this.lineReader  = lineReader;
			this.interpState = interpState;
			this.emitter = emitter;
		}
		
		/** Read at least one operation */
		List<Object> readOps() {
			ArrayList<Object> ops = new ArrayList<Object>();
			while( true ) {
				String line;
				try {
					line = lineReader.readLine();
				} catch (IOException e) {
					throw new RuntimeException("Failed to read ops from input");
				}
				if( line == null ) {
					return ops;
				}
				
				line = line.trim();
				// This would be a good place to look at #lang lines
				if( line.startsWith("#") ) continue;
				if( line.isEmpty() ) continue;
				
				String[] tokens = line.split("\\s+");
				if( tokens.length == 0 ) continue; // Probably shouldn't happen

				Object op = STANDARD_OPS.get(tokens[0]);
				if( op != null ) {
					if( tokens.length > 1 ) {
						throw new RuntimeException("Too many arguments to non-constructor op '"+tokens[0]+"'");
					}
					ops.add(op);
				} else if( OP_CONCAT_N.equals(tokens[0]) ) {
					// TODO: Move these to STANDARD_OPS
					ops.add(ConcatNOp.instance);
				} else if( OP_COUNT_TO_MARK.equals(tokens[0]) ) {
					ops.add(new CountToOp(MARK));
				} else if( OP_PRINT.equals(tokens[0]) ) {
					// For now, print is just an emit
					ops.add(PopAndEmitOp.instance);
				} else if( OP_PRINT_LINE.equals(tokens[0]) ) {
					// For now, print is just an emit
					ops.add(PopAndEmitOp.instance);
					ops.add(new PushOp<Object>("\n"));
					ops.add(PopAndEmitOp.instance);
				} else if( OPC_PUSH_VALUE.equals(tokens[0]) ) {
					Object value = decodeTs34(tokens,1);
					ops.add(new PushOp<Object>(value));
				} else if( OPC_PUSH_SYMBOL.equals(tokens[0]) ) {
					if( tokens.length != 2 ) {
						throw new RuntimeException(OPC_PUSH_SYMBOL+" requires exactly one argument");
					}
					ops.add(new PushOp<Symbol>(new Symbol(tokens[1])));
				} else if( OP_PUSH_MARK.equals(tokens[0]) ) {
					ops.add(new PushOp<Symbol>(MARK));
				} else if( OP_RETURN.equals(tokens[0]) ) {
					ops.add(ReturnOp.instance);
				} else if( OP_QUIT_WITH_CODE.equals(tokens[0]) ) {
					ops.add(PopAndQuitWithCodeOp.instance);
				} else {
					throw new RuntimeException("Unrecognized op: "+line);
				}
				return ops;
			}
		}
		
		/**
		 * Decode and handle request,
		 * transforming it into either a ResumeWith
		 * or some control-flow altering request.
		 */
		protected Object decodeRequest(Object request) {
			if( request == null ) {
				// Nothing to do, booyah!
			} else if( request == readOpsRequest ) {
				if( interactive ) {
					this.emitter.accept("# TS34.19> ");
				}
				List<Object> ops = readOps();
				return new ResumeWith<List<Object>>(ops);
			} else if( request instanceof Absorb<?> ) {
				// Ignore channel for now
				String line;
				try {
					line = this.lineReader.readLine();
				} catch (IOException e) {
					return new IOException("Failed to absorb from input due to IOException", e);
				}
				if( line == null ) {
					return new IOException("End of input");
				} else {
					return new ResumeWith<String>(line);
				}
			}
			return request;
		}
		
		public Integer execute() {
			while( true ) {
				Object request = interpState.getRequest();
				Object decoded = decodeRequest(request);
				if( request instanceof Emit<?,?> ) {
					emitter.accept(((Emit<?,?>)request).value);
					decoded = ResumeWith.blank;
				}
				if( decoded == null ) {
					interpState = interpState.advance(null, 100);
				} else if( decoded instanceof ResumeWith<?> ) {
					interpState = interpState.advance(((ResumeWith<?>)decoded).value, 100);
				} else if( decoded instanceof ReturnWithValue<?> ) {
					@SuppressWarnings("unchecked")
					List<Object> stack = ((ReturnWithValue<List<Object>>)decoded).value;
					this.emitter.accept("# Exiting due to "+decoded+"\n");
					return toInt( stack.get(stack.size()-1) );
				} else if( decoded instanceof QuitWithCode ) {
					this.emitter.accept("# Exiting due to "+decoded+"\n");
					return toInt( ((QuitWithCode)decoded).exitCode );
				} else if( decoded instanceof Exception ) {
					System.err.println("Exception while interpreting program");
					((Exception)decoded).printStackTrace(System.err);
					return EXITCODE_EXCEPTION;
				} else {
					throw new RuntimeException("Don't know how to handle "+decoded);
				}
			}
		}
	}
	
	enum TopLevelReturnHandlingMode {
		QUIT_PROC,
		RETURN_EFFECT
	}
	
	// There are two seemingly valid ways to handle
	// a 'return' at the top level of the program.
	// - QUIT_PROC :: Provide a returnTo continuation which
	//   is a procedure that simply quits (with the assumption
	//   that you put a number on top of the stack beforehand)
	// - RETURN_EFFECT :: Provide no returnTo, forcing the
	//   interpreter to request a return effect, which the
	//   harness handles in the same way it handles a quit effect.
	static TopLevelReturnHandlingMode tlrhm = TopLevelReturnHandlingMode.QUIT_PROC;
	
	public static int runProgramFrom(
		InputStream inputStream,
		boolean interactive,
		Consumer<Object> emitter
	) {
		BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
		
		List<StackyBlockOp<Object,Object>> onReturnProgram = new ArrayList<StackyBlockOp<Object,Object>>();
		if( tlrhm == TopLevelReturnHandlingMode.QUIT_PROC ) {
			onReturnProgram.add(PopAndQuitWithCodeOp.instance);
		} else {
			onReturnProgram.add(ReturnOp.instance);
		}
		
		List<StackyBlockOp<Object,Object>> onEofProgram = new ArrayList<StackyBlockOp<Object,Object>>();
		onEofProgram.add(PushOp.of(0));
		onEofProgram.addAll(onReturnProgram);
		
		List<StackyBlockOp<Object,Object>> program = new ArrayList<StackyBlockOp<Object,Object>>();
		if( interactive ) {
			program.add(PushOp.of("# Hello, world!\n"));
			program.add(PopAndEmitOp.instance);
			program.add(PushOp.of("# This program reads ops from input and executes them.\n"));
			program.add(PopAndEmitOp.instance);
			program.add(PushOp.of("# Please enter your program, below.\n"));
			program.add(PopAndEmitOp.instance);
		}
		
		Continuation<StackyBlockOp<Object,Object>> onReturn = Continuation.to(onReturnProgram, 0, Continuation.exitInterpLoop());
		Continuation<StackyBlockOp<Object,Object>> onEof =  Continuation.to(onEofProgram, 0, Continuation.exitInterpLoop());
		
		Continuation<StackyBlockOp<Object,Object>> reLoop = Continuation.to(program, program.size(), onReturn);
		
		program.addAll(readAndJumpOps(BabbyInterpreterHarness.readOpsRequest, reLoop, onEof));
		
		/*
		program.add(PushOp.of(0));
		program.add(PopAndReturnOp.instance);
		*/
		
		InterpreterState<Object,Object> interpreterState = new StackyBlockInterpreterState<Object, Object>(
			null,
			new Continuation<StackyBlockOp<Object,Object>>(program, 0, reLoop),
			new ArrayList<Object>()
		);
		
		SimpleExecutable<Integer> proc = new BabbyInterpreterHarness(
			interactive,
			br, interpreterState, emitter
		);
		
		return proc.execute().intValue();
	}
	
	static class TestScriptParameters {
		public int expectedExitCode = 0;
	}
	
	static final Pattern DIRECTIVE_PATTERN = Pattern.compile("#(\\S+)(?:\\s*(.*))");
	static TestScriptParameters loadTestScriptParameters(File ts) throws IOException {
		TestScriptParameters params = new TestScriptParameters();
		FileInputStream scriptInputStream = new FileInputStream(ts);
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(scriptInputStream));
			String line;
			while( (line = br.readLine()) != null ) {
				Matcher m;
				if( (m = DIRECTIVE_PATTERN.matcher(line)).matches() ) {
					if( "lang".equals(m.group(1)) ) {
						// No-op
					} else if( "expect-exit-code".equals(m.group(1)) ) {
						params.expectedExitCode = Integer.parseInt(m.group(2));
					} else {
						throw new RuntimeException("Unrecognized directive: "+line);
					}
				}
			}
		} finally {
			scriptInputStream.close();
		}
		return params;
	}
	
	public static int runTestScript(File ts) {
		// System.out.println("# Running test script "+ts);
		try {
			TestScriptParameters params = loadTestScriptParameters(ts);
			FileInputStream scriptInputStream = new FileInputStream(ts);
			try {
				int exitCode = runProgramFrom(scriptInputStream, false, new Consumer<Object>() {
					@Override
					public void accept(Object value) {
						// TODO Collect output and dump to stderr
						// if program returns non-zero
					}
				});
				if( params.expectedExitCode == exitCode ) {
					// System.out.println("# Got correct exit code, "+params.expectedExitCode+", from script "+ts);
					return 0;
				} else {
					System.err.println("Expected exit code "+params.expectedExitCode+" but got "+exitCode+" from script "+ts);
					return 1;
				}
			} finally {
				scriptInputStream.close();
			}
		} catch( Exception e ) {
			System.err.println("Exception while running "+ts);
			e.printStackTrace(System.err);
			return 1;
		}
	}
	
	public static int runTestScripts(File dir) {
		File[] files = dir.listFiles();
		int count = 0;
		int errorCount = 0;
		for( File file : files ) {
			if( file.getName().endsWith(".ts34") ) {
				errorCount += (runTestScript(file) != 0) ? 1 : 0;
				++count;
			}
		}
		if( count == 0 ) {
			System.err.println("No test scripts found in "+dir);
			return 1;
		}
		System.out.println("# Ran "+count+" test scripts, got "+errorCount+" failures");
		if( errorCount > 0 ) {
			System.err.println("Some tests failed");
		}
		return errorCount == 0 ? 0 : 1;
	}
	
	public static void main(String[] args) throws Exception {
		for( int i=0; i<args.length; ++i ) {
			if( "--version".equals(args[i]) ) {
				System.out.println(NAME+"-v"+VERSION);
			} else if( "--return-handling=return-effect".equals(args[i]) ) {
				tlrhm = TopLevelReturnHandlingMode.RETURN_EFFECT;
			} else if( "--return-handling=quit-procedure".equals(args[i]) ) {
				tlrhm = TopLevelReturnHandlingMode.QUIT_PROC;
			} else if( args[i].startsWith("-") && !"-".equals(args[i]) && !"-i".equals(args[i])) {
				System.err.println("Bad arg: "+args[i]);
				System.exit(1);
			} else {
				String scriptFilePath = args[i++];
				List<String> argv = new ArrayList<String>();
				while( i < args.length ) {
					argv.add(args[i++]);
				}
				boolean interactive = false;
				InputStream inputStream;
				if( "-i".equals(scriptFilePath) ) {
					interactive = true;
					inputStream = System.in;
				} else if( "-".equals(scriptFilePath) ) {
					inputStream = System.in;
				} else {
					File f = new File(scriptFilePath);
					if( f.isDirectory() ) {
						System.exit(runTestScripts(f));
					}
					inputStream = getInputStream(scriptFilePath);
				}
				
				final OutputStream os = System.out;
				Consumer<Object> emitter = new Consumer<Object>() {
					public void accept(Object obj) {
						try {
							writeTo(obj, os);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				};
				

				System.exit(runProgramFrom(inputStream, interactive, emitter));
			}
		}
	}
}
