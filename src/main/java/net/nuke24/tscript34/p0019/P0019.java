package net.nuke24.tscript34.p0019;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.nuke24.tscript34.p0019.effect.Absorb;
import net.nuke24.tscript34.p0019.effect.Emit;
import net.nuke24.tscript34.p0019.effect.EndOfProgramReached;
import net.nuke24.tscript34.p0019.effect.ParseException;
import net.nuke24.tscript34.p0019.effect.QuitWithCode;
import net.nuke24.tscript34.p0019.effect.ResumeWith;
import net.nuke24.tscript34.p0019.effect.ReturnWithValue;
import net.nuke24.tscript34.p0019.effect.StackUnderflowException;
import net.nuke24.tscript34.p0019.effect.StoreData;
import net.nuke24.tscript34.p0019.iface.Consumer;
import net.nuke24.tscript34.p0019.iface.InterpreterState;
import net.nuke24.tscript34.p0019.util.Charsets;
import net.nuke24.tscript34.p0019.util.DebugFormat;
import net.nuke24.tscript34.p0019.value.Concatenation;
import net.nuke24.tscript34.p0019.value.Symbol;

public class P0019
{
	public static final String VERSION = "0.2.5-dev";
	public static final String PROGRAM_NAME = "TScript34-P0019-"+VERSION;

	public static final Symbol MARK = new Symbol("http://ns.nuke24.net/TScript34/P0019/Constants/Mark");
		
	public static final int EXIT_CODE_NORMAL = 0;
	public static final int EXIT_CODE_EXCEPTION = 1;
	public static final int EXIT_CODE_USAGE_ERROR = 2; // 'Misuse of shell built-in', also used by JCR36
	public static final int EXIT_CODE_TEST_SCRIPT_UNEXPECTED_EXIT_CODE1 = 4;
	public static final int EXIT_CODE_TEST_SCRIPT_OUTPUT_MISMATCH = 8;
	public static final int EXIT_CODE_PIPING_ERROR = 23; // From JCR36
	// Borrowing some 'standard Linux exit codes':
	public static final int EXIT_CODE_COMMAND_NOT_FOUND = 127; // From JCR36
	public static final int EXIT_CODE_INVALID_EXIT_ARGUMENT = 128; // From JCR36
	// Program tried to read from input, but input's closed
	public static final int EXIT_CODE_READ_END = -15;
	
	public static String OP_OPEN_PROC = "http://ns.nuke24.net/TScript34/Ops/OpenProcedure";
	public static String OP_CLOSE_PROC = "http://ns.nuke24.net/TScript34/Ops/CloseProcedure";
	
	public static String OPC_ALIAS = "http://ns.nuke24.net/TScript34/Op/Alias";
	public static String OPC_PUSH_VALUE = "http://ns.nuke24.net/TScript34/Op/PushValue";
	public static String OPC_PUSH_SYMBOL = "http://ns.nuke24.net/TScript34/Op/PushSymbol";
	
	// <name>:<op constructor arg count>:<pop count>:<push count>
	public static String OP_ABSORB = "http://ns.nuke24.net/TScript34/Ops/Absorb";
	public static String OP_ARRAY_FROM_STACK = "http://ns.nuke24.net/TScript34/Ops/ArrayFromStack";
	public static String OP_CONCAT_N = "http://ns.nuke24.net/TScript34/P0009/Ops/ConcatN"; // item0 item1 ... itemN n -- concatenated
	public static String OP_COUNT_TO_MARK = "http://ns.nuke24.net/TScript34/Ops/CountToMark";
	public static String OP_DROP = "http://ns.nuke24.net/TScript34/Ops/Drop";
	public static String OP_DUP = "http://ns.nuke24.net/TScript34/Ops/Dup";
	public static String OP_EMIT = "http://ns.nuke24.net/TScript34/Ops/Emit";
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
			return new Symbol(uri);
			//throw new RuntimeException("Unrecognized URI '"+uri+"'");
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
	
	interface StackyBlockOp {
		/**
		 * Instructions can directly read and modify the stack.
		 * Anything else requires returning an effect.
		 * Return an effect to request an effect and have
		 * the return value (if the effect calls for one) pushed onto the stack.
		 * Return null to not request an effect.
		 */
		public Object execute(List<Object> stack);
	}
	static class BeginBlockOp implements StackyBlockOp {
		public final EndBlockOp instance = new EndBlockOp();
		private BeginBlockOp() { }
		@Override public Object execute(List<Object> stack) {
			return new RuntimeException("Begin block op should not be executed");
		}
	}
	static class DropOp implements StackyBlockOp {
		public static final DropOp instance = new DropOp();
		private DropOp() { }
		@Override public Object execute(List<Object> stack) {
			if( stack.size() < 1 ) {
				return new StackUnderflowException("Stack underflow; Drop requires one item");
			}
			stack.remove(stack.size()-1);
			return null;
		}
	}
	static class DupOp<V> implements StackyBlockOp {
		public static final DupOp<Object> instance = new DupOp<Object>();
		private DupOp() { }
		@Override public Object execute(List<Object> stack) {
			if( stack.size() < 1 ) {
				return new StackUnderflowException("Stack underflow; Dup requires one item");
			}
			stack.add(stack.get(stack.size()-1));
			return null;
		}
	}
	static class ExchOp<V> implements StackyBlockOp {
		public static final ExchOp<Object> instance = new ExchOp<Object>();
		private ExchOp() { }
		@Override public Object execute(List<Object> stack) {
			if( stack.size() < 2 ) {
				return new StackUnderflowException("Stack underflow; Exch requires two items");
			}
			Object a = stack.get(stack.size()-1);
			stack.set(stack.size()-1, stack.get(stack.size()-2));
			stack.set(stack.size()-1, a);
			return null;
		}
	}
	static class EndBlockOp implements StackyBlockOp {
		public final EndBlockOp instance = new EndBlockOp();
		private EndBlockOp() { }
		public Object execute(List<Object> stack) {
			return new RuntimeException("End block op should not be executed");
		}
	}
	/** -- value */
	static class PushOp implements StackyBlockOp {
		public static PushOp of(Object value) {
			return new PushOp(value);
		}
		final Object value;
		private PushOp(Object value) {
			this.value = value;
		}
		@Override public Object execute(List<Object> stack) {
			stack.add(value);
			return null;
		}
	}
	/** thing --{ emit thing }-- */
	static class PopAndEmitOp implements StackyBlockOp {
		public static final PopAndEmitOp instance = new PopAndEmitOp();
		private PopAndEmitOp() {}
		@Override public Object execute(List<Object> stack) {
			if( stack.size() < 1 ) {
				return new StackUnderflowException("Emit requires one item on the stack");
			}
			Object v = stack.remove(stack.size()-1);
			return new Emit<Object, Object>(null, v);
		}
	}
	static class EffectOp implements StackyBlockOp {
		final Object effect;
		public EffectOp(Object effect) {
			this.effect = effect;
		}
		@Override public Object execute(List<Object> stack) {
			return effect;
		}
	}
	// Pops a return continuation and list of ops from the stack and jumps to it!
	// (op-list return-continuation --)  
	static class PopAndJumpOp<V> implements StackyBlockOp {
		public static final PopAndJumpOp<Object> instance = new PopAndJumpOp<Object>();
		private PopAndJumpOp() {}
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override public Object execute(List<Object> stack) {
			Object contObj = stack.remove(stack.size()-1);
			Object opsObj = stack.remove(stack.size()-1);
			try {
				Continuation<StackyBlockOp> cont = (Continuation<StackyBlockOp>)contObj;
				List<?> ops = (List<?>)opsObj;
				return new Continuation(ops, 0, cont);
			} catch( ClassCastException e ) {
				// TODO: Maybe wrap in a ScriptException or something
				return e;
			}
		}
	}
	static class ReturnOp<V> implements StackyBlockOp {
		public static final ReturnOp<Object> instance = new ReturnOp<Object>();
		private ReturnOp() {}
		@Override public Object execute(List<Object> stack) {
			return new ReturnWithValue<Object>(Collections.unmodifiableList(stack));
		}
	}
	static class PopAndQuitWithCodeOp implements StackyBlockOp {
		public static final PopAndQuitWithCodeOp instance = new PopAndQuitWithCodeOp();
		private PopAndQuitWithCodeOp() {}
		@Override public Object execute(List<Object> stack) {
			Object v = stack.remove(stack.size()-1);
			return new QuitWithCode(toInt(v));
		}
	}
	// (sequence item -- sequence+item)
	static class AppendOp implements StackyBlockOp {
		public static final AppendOp instance = new AppendOp();
		@Override
		public Object execute(List<Object> stack) {
			Object item = stack.remove(stack.size()-1);
			Object hopefullyAList = stack.remove(stack.size()-1);
			if( !(hopefullyAList instanceof List<?>) ) {
				return new RuntimeException("Append, for now, requires sequence argument to implement List; got "+DebugFormat.toDebugString(hopefullyAList));
			}
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>)hopefullyAList;
			List<Object> newList = new ArrayList<Object>(list);
			newList.add(item);
			stack.add(newList);
			return null;
		}
	}
	static class ConcatNOp implements StackyBlockOp {
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
	static class CountToOp implements StackyBlockOp {
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
	
	static final Map<String,StackyBlockOp> STANDARD_OPS = new HashMap<String,StackyBlockOp>();
	static {
		STANDARD_OPS.put(OP_ABSORB, new EffectOp(new Absorb<Integer>(0)));
		STANDARD_OPS.put(OP_DROP, DropOp.instance);
		STANDARD_OPS.put(OP_DUP ,  DupOp.instance);
		STANDARD_OPS.put(OP_EXCH, ExchOp.instance);
		
		STANDARD_OPS.put(OP_QUIT, new EffectOp(new QuitWithCode(0)));
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
	
	static class StackyBlockInterpreterState implements InterpreterState<Object, Object> {
		public static final Object PushCurrentReturnContinuation = new Object();
		
		// Might replace this with a stack of them, i.e. the return stack
		final Object request;
		final Continuation<StackyBlockOp> next;
		final List<Object> dataStack;
		
		public StackyBlockInterpreterState(
			Object request,
			Continuation<StackyBlockOp> next,
			List<Object> dataStack
		) {
			this.request = request;
			this.next = next;
			this.dataStack = dataStack;
		}
		@Override public Object getRequest() { return request; }
		@SuppressWarnings("unchecked")
		@Override
		public InterpreterState<Object,Object> advance(Object arg, int maxSteps) {
			List<StackyBlockOp> instructions = next.block;
			List<Object> stack = new ArrayList<Object>(dataStack);
			Continuation<StackyBlockOp> returnTo = next.returnTo;
			if( arg != null ) {
				stack.add(arg);
			}
			int ip = next.index;
			Object request = null;
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
					return new ReturnInterpreterState<Object,Object>(new ReturnWithValue<List<Object>>(Collections.unmodifiableList(stack)));
				}
				if( ip < 0 ) {
					throw new RuntimeException("Bad instruction pointer: "+ip);
				}
				request = instructions.get(ip++).execute(stack);
				if( request == PushCurrentReturnContinuation ) {
					stack.add(returnTo);
					request = null;
				} else if( request instanceof ReturnWithValue<?> ) {
					ReturnWithValue<List<Object>> ret = (ReturnWithValue<List<Object>>)request;
					if( ret.value == null || !(ret.value instanceof List<?>) ) {
						throw new RuntimeException("ReturnWithValue value should be a whole stack, a List<Object>; got "+DebugFormat.toDebugString(ret.value));
					}
					stack = new ArrayList<Object>(ret.value);
					request = returnTo;
				}
				// Continuation = JumpTo(Continuation);
				// I just was lazy and didn't want to add a new object.
				if( request instanceof Continuation ) {
					Continuation<StackyBlockOp> continuation = (Continuation<StackyBlockOp>)request;
					instructions = continuation.block;
					ip = continuation.index;
					returnTo = continuation.returnTo;
					request = null;
				}
			}
			Continuation<StackyBlockOp> continuation;
			if( request == null ) {
				assert ip == instructions.size();
				continuation = returnTo;
				System.err.println("Reached end of program at ip = "+ip);
				// Reached to end of program!
				// Let's say for now that this is an error.
				// If you want to return, add a return op.
				request = new EndOfProgramReached();
				// actually for now, let reaching end of program
				// is an implicit return, to make the read-execute loop
				// work even though it's not explicitly adding
				// return ops to the end of op lists
				// (since it doesn't know if it's immediately executing
				// or making a procedure, asjndkajsndkaj)
			} else {
				continuation = new Continuation<StackyBlockOp>(instructions, ip, returnTo);
			}
			return new StackyBlockInterpreterState(
				request,
				continuation,
				Collections.unmodifiableList(stack)
			);
		}
	}
	
	@SuppressWarnings("unchecked")
	static List<StackyBlockOp> readAndJumpOps(
		final Object readOpsRequest,
		final Continuation<StackyBlockOp> andThen,
		final Continuation<StackyBlockOp> onEof
	) {
		final ArrayList<StackyBlockOp> rjOps = new ArrayList<StackyBlockOp>();
		rjOps.add(new EffectOp(readOpsRequest));
		rjOps.add(new StackyBlockOp() {
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
		rjOps.add(PushOp.of(new EffectOp(andThen)));
		// newOps jumpToContinuatoin 
		rjOps.add(AppendOp.instance);
		// newOps+jumpToContinuation
		rjOps.add(new EffectOp(StackyBlockInterpreterState.PushCurrentReturnContinuation));
		// newOps+jumpToContinuation returnTo
		rjOps.add((StackyBlockOp)PopAndJumpOp.instance);
		return rjOps;
	}
	
	interface SimpleExecutable<R> {
		R execute();
	}
	
	interface ZReader {
		public String readLine() throws IOException;
		public byte[] readChunk(int maxLen) throws IOException;
	}
	static class InputStreamZReader implements ZReader, ItemReader<String> {
		static final byte[] EMPTY_BUF = new byte[0];
		
		byte[] buffered = EMPTY_BUF;
		byte[] readBuf = new byte[65536];
		final InputStream is;
		public InputStreamZReader(InputStream is) {
			this.is = is;
		}
		
		/**
		 * Read a chunk of at least one byte,
		 * unless at EOF, in which case an empty byte array will be returned.
		 */
		public byte[] readChunk(int maxLen) throws IOException {
			if( this.buffered.length > 0 ) {
				if( this.buffered.length > maxLen ) {
					try {
						return Arrays.copyOf(this.buffered, maxLen);
					} finally {
						this.buffered = Arrays.copyOfRange(this.buffered, maxLen, this.buffered.length);
					}
				}
				try {
					return this.buffered;
				} finally {
					this.buffered = EMPTY_BUF;
				}
			}
			int z = is.read(this.readBuf, 0, Math.min(maxLen, this.readBuf.length));
			if( z <= 0 ) return EMPTY_BUF;
			if( z == this.readBuf.length ) {
				try {
					return this.readBuf;
				} finally {
					this.readBuf = new byte[65536];
				}
			} else {
				return Arrays.copyOf(this.readBuf, z);
			}
		}
		
		protected byte[] readChunk() throws IOException {
			return this.readChunk(this.readBuf.length);
		}
		
		protected void unshift(byte[] data) {
			if( this.buffered.length > 0 ) {
				// Can change to handle this case if needed
				throw new RuntimeException("Can't unshift; some data already buffered!");
			}
			this.buffered = data;
		}
		
		@Override
		public String readLine() throws IOException {
			byte[] lineBuf = readChunk();
			if( lineBuf.length == 0 ) return null;
			
			int lineBufEnd = lineBuf.length;
			
			int chunkReadOffset = lineBuf.length;
			int searchIndex=0;
			while( true ) {
				for( ; searchIndex<lineBufEnd; ++searchIndex ) {
					if( lineBuf[searchIndex] == '\n' ) {
						try {
							return new String(lineBuf, 0, searchIndex, Charsets.UTF8);
						} finally {
							unshift(Arrays.copyOfRange(lineBuf, searchIndex+1, chunkReadOffset));
						}
					}
				}
				
				// Reached end of current lineBuf without finding it.
				// Need to buffer more!
				
				byte[] more = readChunk();
				if( more.length == 0 ) {
					// At EOF!  What we have is all there is.
					return new String(lineBuf, 0, lineBufEnd);
				}
				
				// Expand lineBuf, read more into it, and continue searching.
				lineBuf = Arrays.copyOf(lineBuf, lineBufEnd * 2 + more.length);
				for( int j=0; j<more.length; ++j ) {
					lineBuf[lineBufEnd++] = more[j];
				}
			}
		}
		
		@Override
		public String read() throws IOException {
			return readLine();
		}
	}
	
	interface ItemReader<T> {
		T read() throws IOException; 
	}
	interface BulkItemReader<T> {
		List<T> read() throws IOException; 
	}
	static class IteratorItemReader<T> implements ItemReader<T> {
		protected final Iterator<? extends T> it;
		public IteratorItemReader(Iterator<? extends T> it) {
			this.it = it;
		}
		@Override
		public T read() throws IOException {
			try {
				return it.next();
			} catch( NoSuchElementException e ) {
				throw new IOException("End of input", e);
			}
		}
	}
	
	static class BabbyOpReader implements BulkItemReader<Object> {
		protected final ZReader lineReader;
		int lineNumber = 0;
		
		public BabbyOpReader(ZReader lineReader) {
			this.lineReader = lineReader;
		}
		
		static final Pattern ID_OPTION_PATTERN = Pattern.compile("--id=(.*)");
		static final Pattern SECTOR_OPTION_PATTERN = Pattern.compile("--sector=(.*)");
		
		/** Read at least one operation */
		public List<Object> read() throws IOException {
			ArrayList<Object> ops = new ArrayList<Object>();
			while( true ) {
				String line;
				try {
					++lineNumber;
					line = lineReader.readLine();
				} catch (IOException e) {
					throw new RuntimeException("Failed to read ops from input");
				}
				if( line == null ) {
					return ops;
				}
				
				line = line.trim();
				if( line.startsWith("#CHUNK") ) {
					String[] tokens = line.split("\\s+");
					if( tokens.length < 2 ) {
						throw new RuntimeException("Bad #CHUNK directive lacks size: "+line);
					}
					String id = null;
					String sectorName = StoreData.SECTOR_SCRIPT_LOCAL;
					for( int i=2; i<tokens.length; ++i ) {
						Matcher m;
						if( (m = ID_OPTION_PATTERN.matcher(tokens[i])).matches() ) {
							id = m.group(1);
						} else if( (m = SECTOR_OPTION_PATTERN.matcher(tokens[i])).matches() ) {
							sectorName = m.group(1);
						} else {
							throw new RuntimeException("Unrecognized #CHUNK option: '"+tokens[i]+"'");
						}
					}
					int size;
					try {
						size = Integer.parseInt(tokens[1]);
					} catch( NumberFormatException e ) {
						throw new RuntimeException(e);
					}
					List<byte[]> chunks = new ArrayList<byte[]>();
					int read = 0;
					while( read < size ) {
						byte[] chunk = lineReader.readChunk(size - read);
						chunks.add(chunk);
						read += chunk.length;
					}
					// Could just store the chunk instead of treating it as an op!
					// Or, if it is represented as an op, maybe it should be an
					// explicitly compile-time one.
					// Even if a #CHUNK appears in a loop, only need to store once,
					// as it has no effect on the stack.
					ops.add(new EffectOp(
						new StoreData(id, sectorName, new Concatenation<Byte>(chunks.toArray()))
					));
					return ops;
				}
				if( line.startsWith("#ENDCHUNK") ) {
					// Cool, cool.
					// Presumably this follows a #CHUNK.
					// TODO: Enforce #CHUNK ... #ENDCHUNK ordering.
					continue;
				}
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
					ops.add(PushOp.of("\n"));
					ops.add(PopAndEmitOp.instance);
				} else if( OPC_PUSH_VALUE.equals(tokens[0]) ) {
					Object value = decodeTs34(tokens,1);
					ops.add(PushOp.of(value));
				} else if( OPC_PUSH_SYMBOL.equals(tokens[0]) ) {
					if( tokens.length != 2 ) {
						throw new RuntimeException(OPC_PUSH_SYMBOL+" requires exactly one argument");
					}
					ops.add(PushOp.of(new Symbol(tokens[1])));
				} else if( OP_PUSH_MARK.equals(tokens[0]) ) {
					ops.add(PushOp.of(MARK));
				} else if( OP_RETURN.equals(tokens[0]) ) {
					ops.add(ReturnOp.instance);
				} else if( OP_QUIT_WITH_CODE.equals(tokens[0]) ) {
					ops.add(PopAndQuitWithCodeOp.instance);
				} else {
					throw new ParseException("Unrecognized op: "+line, "?", lineNumber);
				}
				return ops;
			}
		}
	}
	
	/** Exercse the InterpreterState API in a minimal way
	 * 
	 * Passing the emitter as the 'context'
	 * for now.  Maybe it should just be built-in like
	 * the other thingies, or maybe they should uhm....
	 * */
	static class BabbyInterpreterHarness implements SimpleExecutable<Integer> {
		public static final Object readOpsRequest = new Object();
		
		// For now there's just one input channel,
		// and it's from where the script is read
		// TODO: Pass a separate ops reader
		private final boolean interactive;
		private final BulkItemReader<?> opReader;
		private final ItemReader<?> inputReader;
		private InterpreterState<Object, ?> interpState;
		private final Consumer<Object> emitter;
		public BabbyInterpreterHarness(
			boolean interactive,
			BulkItemReader<?> opReader,
			ItemReader<?> inputReader,
			InterpreterState<Object,?> interpState,
			Consumer<Object> emitter
		) {
			this.interactive = interactive;
			this.opReader = opReader;
			this.inputReader = inputReader;
			this.interpState = interpState;
			this.emitter = emitter;
		}
		
		// For now just stick stored data in a map!
		final Map<String,Object> datastore = new HashMap<String,Object>();
		
		protected Object decodeForEmission(Object value) {
			// Necessary because the thing that serializes
			// output is outside of this harness, so doesn't
			// know about the symbols.
			// Make it work.
			if( value instanceof Concatenation<?> ) {
				Concatenation<?> cat = (Concatenation<?>)value;
				List<Object> decoded = new ArrayList<Object>();
				for( Object element : cat.children ) {
					Object dec = decodeForEmission(element);
					decoded.add(dec);
				}
				return new Concatenation<Object>(decoded.toArray());
			}
			
			if( value instanceof Symbol ) {
				Symbol sym = (Symbol)value;
				Object res = this.datastore.get(sym.name);
				if( res != null && res != sym ) {
					// Warning: potential for infinite recursion if a -> b -> a
					return decodeForEmission(res);
				}
			}
			
			return value;
		}
		
		/**
		 * Decode and handle request,
		 * transforming it into either a ResumeWith
		 * or some control-flow altering request.
		 */
		protected Object decodeRequest(Object request) {
			if( request == null ) {
				// Nothing to do, booyah!
				return ResumeWith.blank;
			} else if( request == readOpsRequest ) {
				while( true ) {
					if( interactive ) {
						this.emitter.accept("# TS34.19> ");
					}
					try {
						return new ResumeWith<List<?>>(opReader.read());
					} catch( ParseException e ) {
						if( interactive ) {
							this.emitter.accept("# "+e+"\n");
							continue;
						} else {
							throw new RuntimeException(e);
						}
					} catch(IOException e) {
						return e;
					}
				}
			} else if( request instanceof Absorb<?> ) {
				// Ignore channel for now
				Object item;
				try {
					item = this.inputReader.read();
				} catch (IOException e) {
					return new IOException("Failed to absorb from input due to IOException", e);
				}
				if( item == null ) {
					return new IOException("End of input");
				} else {
					return new ResumeWith<Object>(item);
				}
			} else if( request instanceof Emit<?,?> ) {
				Object value = ((Emit<?,?>)request).value;
				value = decodeForEmission(value);
				emitter.accept(value);
				return ResumeWith.blank;
			} else if( request instanceof StoreData ) {
				StoreData storeData = (StoreData)request;
				if( storeData.id == null ) {
					// Could calculate bitprint URN.
					return new RuntimeException("Storage request currently requires ID");
				}
				if( this.datastore.containsKey(storeData.id) ) {
					// Maybe actually okay if value is identical.
					return new RuntimeException("Duplicate item ID: "+storeData.id);
				}
				this.datastore.put(storeData.id, storeData.data);
				return ResumeWith.blank;
			}
			return request;
		}
		
		public Integer execute() {
			while( true ) {
				Object request = interpState.getRequest();
				Object decoded = decodeRequest(request);
				/*if( decoded == null ) {
					// Why allow null when ResumeWith.blank exists?
					interpState = interpState.advance(null, 100);
				} else */
				if( decoded instanceof ResumeWith<?> ) {
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
					return EXIT_CODE_EXCEPTION;
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
		final BulkItemReader<Object> opReader,
		final ItemReader<? extends Object> itemReader,
		final boolean interactive,
		final Consumer<Object> emitter
	) {
		List<StackyBlockOp> onReturnProgram = new ArrayList<StackyBlockOp>();
		if( tlrhm == TopLevelReturnHandlingMode.QUIT_PROC ) {
			onReturnProgram.add(PopAndQuitWithCodeOp.instance);
		} else {
			onReturnProgram.add(ReturnOp.instance);
		}
		
		List<StackyBlockOp> onEofProgram = new ArrayList<StackyBlockOp>();
		onEofProgram.add(PushOp.of(0));
		onEofProgram.addAll(onReturnProgram);
		
		List<StackyBlockOp> program = new ArrayList<StackyBlockOp>();
		if( interactive ) {
			program.add(PushOp.of("# Hello, world!\n"));
			program.add(PopAndEmitOp.instance);
			program.add(PushOp.of("# This program reads ops from input and executes them.\n"));
			program.add(PopAndEmitOp.instance);
			program.add(PushOp.of("# Please enter your program, below.\n"));
			program.add(PopAndEmitOp.instance);
		}
		
		Continuation<StackyBlockOp> onReturn = Continuation.<StackyBlockOp>to(onReturnProgram, 0, Continuation.<StackyBlockOp>exitInterpLoop());
		Continuation<StackyBlockOp> onEof =  Continuation.<StackyBlockOp>to(onEofProgram, 0, Continuation.<StackyBlockOp>exitInterpLoop());
		
		Continuation<StackyBlockOp> reLoop = Continuation.<StackyBlockOp>to(program, program.size(), onReturn);
		
		program.addAll(readAndJumpOps(BabbyInterpreterHarness.readOpsRequest, reLoop, onEof));
		
		/*
		program.add(PushOp.of(0));
		program.add(PopAndReturnOp.instance);
		*/
		
		InterpreterState<Object,Object> interpreterState = new StackyBlockInterpreterState(
			null,
			new Continuation<StackyBlockOp>(program, 0, reLoop),
			new ArrayList<Object>()
		);
		
		SimpleExecutable<Integer> proc = new BabbyInterpreterHarness(
			interactive,
			opReader,
			itemReader,
			interpreterState,
			emitter
		);
		
		return proc.execute().intValue();
	}
	
	static class TestScriptParameters {
		public List<String> inputs = new ArrayList<String>();
		public int expectedExitCode = 0;
		public String expectedOutput = null;
	}
	
	static final Pattern DIRECTIVE_PATTERN = Pattern.compile("#(\\S+)(?:\\s*(.*))?");
	@SuppressWarnings("resource")
	static TestScriptParameters loadTestScriptParameters(File ts) throws IOException {
		TestScriptParameters params = new TestScriptParameters();
		FileInputStream scriptInputStream = new FileInputStream(ts);
		try {
			ZReader zreader = new InputStreamZReader(scriptInputStream);
			String line;
			while( (line = zreader.readLine()) != null ) {
				Matcher m;
				if( (m = DIRECTIVE_PATTERN.matcher(line)).matches() ) {
					if( "lang".equals(m.group(1)) ) {
						// No-op
					} else if( "input-line".equals(m.group(1)) ) {
						params.inputs.add(m.group(2));
					} else if( "expect-output".equals(m.group(1)) ) {
						if( params.expectedOutput == null ) {
							params.expectedOutput = "";
						}
						params.expectedOutput += m.group(2) + "\n";
					} else if( "expect-exit-code".equals(m.group(1)) ) {
						params.expectedExitCode = Integer.parseInt(m.group(2));
					} else if( "CHUNK".equals(m.group(1)) ) {
					} else if( "ENDCHUNK".equals(m.group(1)) ) {
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
		try {
			TestScriptParameters params = loadTestScriptParameters(ts);
			FileInputStream scriptInputStream = new FileInputStream(ts);
			final ByteArrayOutputStream output = new ByteArrayOutputStream();
			try {
				BulkItemReader<Object> opReader = new BabbyOpReader(new InputStreamZReader(scriptInputStream));
				ItemReader<Object> itemReader = new IteratorItemReader<Object>(params.inputs.iterator());
				int exitCode = runProgramFrom(opReader, itemReader, false, new Consumer<Object>() {
					@Override
					public void accept(Object value) {
						// HACK: Ignore informational lines
						// TODO: Have a separate 'log' that's not 'emit'
						if( value instanceof String && value.toString().startsWith("# ") ) return;
						
						// TODO: Handle the various objects that might be emitted,
						// translating to bytes!
						try {
							writeTo(value, output);
						} catch( IOException e ) {
							throw new RuntimeException(e);
						}
						// TODO dump output (and error/debug output) to stderr
						// if program returns non-zero
					}
				});
				if( params.expectedExitCode != exitCode ) {
					System.err.println("Expected exit code "+params.expectedExitCode+" but got "+exitCode+" from script "+ts);
					return EXIT_CODE_TEST_SCRIPT_UNEXPECTED_EXIT_CODE1;
				}
				if( params.expectedOutput != null ) {
					String actualOutput = new String(output.toByteArray(), Charsets.UTF8);
					if( !params.expectedOutput.equals(actualOutput) ) {
						System.err.println("Output from "+ts+" did not match expected:");
						System.err.println("-- Expected --");
						System.err.println(params.expectedOutput);
						System.err.println("-- Actual --");
						System.err.println(actualOutput);
						return EXIT_CODE_TEST_SCRIPT_OUTPUT_MISMATCH;
					}
				}
				return EXIT_CODE_NORMAL;
			} finally {
				scriptInputStream.close();
			}
		} catch( Exception e ) {
			System.err.println("Exception while running "+ts);
			e.printStackTrace(System.err);
			return EXIT_CODE_EXCEPTION;
		}
	}
	
	public static int runTestScripts(File dir, PrintStream stdout, PrintStream errout) {
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
			errout.println("No test scripts found in "+dir);
			return EXIT_CODE_TEST_SCRIPT_UNEXPECTED_EXIT_CODE1;
		}
		stdout.println("# Ran "+count+" test scripts, got "+errorCount+" failures");
		if( errorCount > 0 ) {
			errout.println("Some tests failed");
		}
		return errorCount == 0 ? 0 : 1;
	}
	
	public static int scriptMain(String[] args, int argi, InputStream stdin, PrintStream stdout, PrintStream errout) throws Exception {
		for( ; argi<args.length; ++argi ) {
			if( "--version".equals(args[argi]) ) {
				stdout.println(PROGRAM_NAME);
			} else if( "--return-handling=return-effect".equals(args[argi]) ) {
				tlrhm = TopLevelReturnHandlingMode.RETURN_EFFECT;
			} else if( "--return-handling=quit-procedure".equals(args[argi]) ) {
				tlrhm = TopLevelReturnHandlingMode.QUIT_PROC;
			} else if( args[argi].startsWith("-") && !"-".equals(args[argi]) && !"-i".equals(args[argi])) {
				errout.println("Bad arg: "+args[argi]);
				return 1;
			} else {
				String scriptFilePath = args[argi++];
				List<String> argv = new ArrayList<String>();
				while( argi < args.length ) {
					argv.add(args[argi++]);
				}
				boolean interactive = false;
				InputStream inputStream;
				if( "-i".equals(scriptFilePath) ) {
					interactive = true;
					inputStream = stdin;
				} else if( "-".equals(scriptFilePath) ) {
					inputStream = stdin;
				} else {
					File f = new File(scriptFilePath);
					if( f.isDirectory() ) {
						return runTestScripts(f, stdout, errout);
					}
					inputStream = getInputStream(scriptFilePath);
				}
				
				final OutputStream os = stdout;
				Consumer<Object> emitter = new Consumer<Object>() {
					public void accept(Object obj) {
						try {
							writeTo(obj, os);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				};
				
				ZReader opZReader = new InputStreamZReader(inputStream);
				BulkItemReader<Object> opReader = new BabbyOpReader(opZReader);
				InputStreamZReader inputReader = new InputStreamZReader(stdin);
				return runProgramFrom(opReader, inputReader, interactive, emitter);
			}
		}
		
		return 0;
	}
}
