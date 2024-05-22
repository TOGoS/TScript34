package net.nuke24.tscript34.p0019;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.nuke24.tscript34.p0019.effect.Absorb;
import net.nuke24.tscript34.p0019.effect.Emit;
import net.nuke24.tscript34.p0019.effect.EndOfProgramReached;
import net.nuke24.tscript34.p0019.effect.QuitWithCode;
import net.nuke24.tscript34.p0019.effect.ResumeWith;
import net.nuke24.tscript34.p0019.effect.ReturnWithValue;
import net.nuke24.tscript34.p0019.util.Charsets;

// TODO: Use P0010's interfaces
interface Consumer<T> {
	public void accept(T value);
}

public class P0019 {
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
	
	// <name>:<op constructor arg count>:<pop count>:<push count>
	public static String OP_ARRAY_FROM_STACK = "http://ns.nuke24.net/TScript34/Ops/ArrayFromStack";
	public static String OP_CONCAT_N = "http://ns.nuke24.net/TScript34/P0009/Ops/ConcatN"; // item0 item1 ... itemN n -- concatenated
	public static String OP_COUNT_TO_MARK = "http://ns.nuke24.net/TScript34/Ops/CountToMark";
	public static String OP_DUP = "http://ns.nuke24.net/TScript34/Ops/Dup";
	public static String OP_EXCH = "http://ns.nuke24.net/TScript34/Ops/Exch";
	public static String OP_EXECUTE = "http://ns.nuke24.net/TScript34/Ops/Execute";
	public static String OP_GET_INTERPRETER_INFO = "http://ns.nuke24.net/TScript34/Ops/GetInterpreterInfo";
	public static String OP_JUMP = "http://ns.nuke24.net/TScript34/P0009/Ops/Jump";
	public static String OP_POP = "http://ns.nuke24.net/TScript34/Ops/Pop";
	public static String OP_PUSH_MARK = "http://ns.nuke24.net/TScript34/Ops/PushMark";
	public static String OP_PRINT = "http://ns.nuke24.net/TScript34/Ops/Print";
	public static String OP_PRINT_LINE = "http://ns.nuke24.net/TScript34/Ops/PrintLine";
	public static String OP_PRINT_STACK_THUNKS = "http://ns.nuke24.net/TScript34/Ops/PrintStackThunks";
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
			dest.append(obj.toString());
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
			return obj.toString();
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
			System.err.println("Outputting "+obj+" to stream by toString()ing it, which might be wack");
			// TODO: Any other special cases?
			// togos.blob.OutputStreamable, etc?
			if( os instanceof Appendable ) {
				append(obj, (Appendable)os );
			} else {
				os.write(obj.toString().getBytes(Charsets.UTF8));
			}
		}
	}
	
	public static InputStream getInputStream(String pathOrUri) throws IOException {
		// TODO: Check for URIs, being careful about those one-letter
		// URI schemes that are actually Windows file paths.
		// (JCR36 already does this; maybe shove that decoding into a library)
		return new FileInputStream(pathOrUri);
	}
	
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
		public Object execute(List<V> stack) {
			return new RuntimeException("Begin block op should not be executed");
		}
	}
	static class EndBlockOp<V> implements StackyBlockOp<V,Object> {
		public final EndBlockOp<Object> instance = new EndBlockOp<Object>();
		private EndBlockOp() { }
		public Object execute(List<V> stack) {
			return new RuntimeException("End block op should not be executed");
		}
	}
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
	static class PopAndEmitOp<V> implements StackyBlockOp<V,Object> {
		public static final PopAndEmitOp<Object> instance = new PopAndEmitOp<Object>();
		private PopAndEmitOp() {}
		@Override public Object execute(List<V> stack) {
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
			return new ReturnWithValue<Object>(null);
		}
	}
	static class PopAndReturnOp<V> implements StackyBlockOp<V,Object> {
		public static final PopAndReturnOp<Object> instance = new PopAndReturnOp<Object>();
		private PopAndReturnOp() {}
		@Override public Object execute(List<V> stack) {
			Object v = stack.remove(stack.size()-1);
			return new ReturnWithValue<Object>(v);
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
	
	static final class Continuation<I> {
		public static final Continuation<?> RETURN_TO_NONE = new Continuation<Object>(Collections.emptyList(), 0);
		public static final Continuation<?> RETURN_TO_SELF = new Continuation<Object>(Collections.emptyList(), 0);
		
		@SuppressWarnings("unchecked")
		public static <T> Continuation<T> returnToNone() {
			return (Continuation<T>)RETURN_TO_NONE;
		}
		
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
			this.returnTo = null;
		}
		public Continuation(List<I> block, int index, Continuation<I> returnTo) {
			this.block = block;
			this.index = index;
			if( returnTo == RETURN_TO_NONE ) {
				this.returnTo = null;
			} else if( returnTo == RETURN_TO_SELF ) {
				this.returnTo = this;
			} else {
				this.returnTo = returnTo;
			}
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
				request = instructions.get(ip++).execute(stack);
				if( request == PushCurrentReturnContinuation ) {
					stack.add((A)returnTo);
					request = null;
				} else if( request instanceof ReturnWithValue<?> ) {
					ReturnWithValue<A> ret = (ReturnWithValue<A>)request;
					if( returnTo == null ) {
						// By convention, the top value on the stack is 'the return value'.
						// Since we're exiting stack-land, reflect it by putting
						// it into the Return effect:
						if( ret.value == null && this.dataStack.size() > 0 ) {
							ret = new ReturnWithValue<A>(dataStack.get(dataStack.size()-1));
						}
						return new ReturnInterpreterState<A,E>((E)ret);
					} else {
						// Then the return is *to* somewhere.
						// If the return had a value, put it on the stack.
						// (Maybe this would all be clearer if return-with-explicit-value and
						// return-with-implicit-value-on-stack were different types, idk)
						if( ret.value != null ) {
							// Push it to the stack
							stack.add(ret.value);
						}
						request = (E) returnTo;
					}
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
	static List<StackyBlockOp<Object,Object>> readAndJumpOps(Object readOpsRequest, Continuation<StackyBlockOp<Object,Object>> andThen) {
		final ArrayList<StackyBlockOp<Object,Object>> rjOps = new ArrayList<StackyBlockOp<Object,Object>>();
		rjOps.add(new EffectOp<Object,Object>(readOpsRequest));
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
				
				if( OP_PRINT.equals(tokens[0]) ) {
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
					Object rVal = ((ReturnWithValue<?>)decoded).value;
					this.emitter.accept("# Exiting due to "+decoded);
					return toInt( rVal );
				} else if( decoded instanceof QuitWithCode ) {
					this.emitter.accept("# Exiting due to "+decoded );
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
	
	public static void main(String[] args) throws Exception {
		// There are two seemingly valid ways to handle
		// a 'return' at the top level of the program.
		// - QUIT_PROC :: Provide a returnTo continuation which
		//   is a procedure that simply quits (with the assumption
		//   that you put a number on top of the stack beforehand)
		// - RETURN_EFFECT :: Provide no returnTo, forcing the
		//   interpreter to request a return effect, which the
		//   harness handles in the same way it handles a quit effect.
		TopLevelReturnHandlingMode tlrhm = TopLevelReturnHandlingMode.QUIT_PROC;
		
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
					inputStream = getInputStream(scriptFilePath);
				}
				BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
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
				
				List<StackyBlockOp<Object,Object>> onReturn = new ArrayList<StackyBlockOp<Object,Object>>();
				onReturn.add(PopAndQuitWithCodeOp.instance);
				
				List<StackyBlockOp<Object,Object>> program = new ArrayList<StackyBlockOp<Object,Object>>();
				if( interactive ) {
					program.add(PushOp.of("# Hello, world!\n"));
					program.add(PopAndEmitOp.instance);
					program.add(PushOp.of("# This program reads ops from input and executes them.\n"));
					program.add(PopAndEmitOp.instance);
					program.add(PushOp.of("# Please enter your program, below.\n"));
					program.add(PopAndEmitOp.instance);
				}
				Continuation<StackyBlockOp<Object,Object>> reLoop = new Continuation<StackyBlockOp<Object,Object>>(program, program.size(),
					tlrhm == TopLevelReturnHandlingMode.QUIT_PROC
						? new Continuation<StackyBlockOp<Object,Object>>(onReturn, 0, Continuation.returnToNone())
						: Continuation.returnToNone()
					
				);
				program.addAll(readAndJumpOps(BabbyInterpreterHarness.readOpsRequest, reLoop));
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
				System.exit(proc.execute().intValue());
			}
		}
	}
}
