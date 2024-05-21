package net.nuke24.tscript34.p0019;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import net.nuke24.tscript34.p0019.effect.Absorb;
import net.nuke24.tscript34.p0019.effect.Emit;
import net.nuke24.tscript34.p0019.effect.ResumeWith;
import net.nuke24.tscript34.p0019.effect.Return;
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
	public static String OP_RETURN = "return:0:0:0";
	
	public static String DATATYPE_DECIMAL = "http://www.w3.org/2001/XMLSchema#decimal";
	
	protected static final Pattern dataUriPat = Pattern.compile("^data:,(.*)");
	protected static final Pattern decimalIntegerPat = Pattern.compile("^(\\d+)$");
	protected static final Pattern hexIntegerPat = Pattern.compile("^0x([\\da-fA-F]+)$");
	
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
	
	class ReturnInterpreterState<A, E> implements InterpreterState<A, E> {
		final E request;
		public ReturnInterpreterState(E request) {
			this.request = request;
		}
		@Override public E getRequest() { return this.request; }
		@Override public InterpreterState<? super A, ? extends E> advance(A arg, int maxSteps) {
			throw new RuntimeException("Shouldn't've been called!");
		}
	}
	
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
	static class EmitOp<V> implements StackyBlockOp<V,Object> {
		public static final EmitOp<Object> instance = new EmitOp<Object>();
		private EmitOp() {}
		@Override public Object execute(List<V> stack) {
			Object v = stack.remove(stack.size()-1);
			return new Emit<Object, Object>(null, v);
		}
	}
	static class ReturnOp<V> implements StackyBlockOp<V,Object> {
		public static final ReturnOp<Object> instance = new ReturnOp<Object>();
		private ReturnOp() {}
		@Override public Object execute(List<V> stack) {
			Object v = stack.remove(stack.size()-1);
			return new Return<Object>(v);
		}
	}
	
	static final class BlockInstructionPointer<I> {
		public final List<I> block;
		public final int index;
		public BlockInstructionPointer(List<I> block, int index) {
			this.block = block;
			this.index = index;
		}
	}
	
	static class StackyBlockInterpreterState<A,E> implements InterpreterState<A, E> {
		// Might replace this with a stack of them, i.e. the return stack
		final E request;
		final BlockInstructionPointer<StackyBlockOp<A,E>> next;
		final List<A> dataStack;
		
		public StackyBlockInterpreterState(
			E request,
			BlockInstructionPointer<StackyBlockOp<A,E>> next,
			List<A> dataStack
		) {
			this.request = request;
			this.next = next;
			this.dataStack = dataStack;
		}
		@Override public E getRequest() { return request; }
		@Override
		public InterpreterState<? super A, ? extends E> advance(A arg, int maxSteps) {
			List<StackyBlockOp<A,E>> instructions = next.block;
			List<A> stack = new ArrayList<A>(dataStack);
			if( arg != null ) {
				stack.add(arg);
			}
			int ip = next.index;
			E request = null;
			while( request == null && ip < instructions.size() && maxSteps-- > 0 ) {
				request = instructions.get(ip++).execute(stack);
			}
			if( request == null ) {
				// Reached to end of program!
				// Let's say for now that this is an error.
				request = (E)new Exception("Reached end of program");
			}
			return new StackyBlockInterpreterState<A,E>(
				request,
				new BlockInstructionPointer<StackyBlockOp<A, E>>(instructions, ip),
				Collections.unmodifiableList(stack)
			);
		}
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
		private final BufferedReader lineReader;
		private InterpreterState<Object, ?> interpState;
		private final Consumer<Object> emitter;
		public BabbyInterpreterHarness(
			BufferedReader lineReader,
			InterpreterState<Object,?> interpState,
			Consumer<Object> emitter
		) {
			this.lineReader  = lineReader;
			this.interpState = interpState;
			this.emitter = emitter;
		}
		
		/**
		 * Decode and handle request,
		 * transforming it into either a ResumeWith
		 * or some control-flow altering request.
		 */
		protected Object decodeRequest(Object request) {
			if( request == null ) {
				// Nothing to do, booyah!
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
				} else if( decoded instanceof Return<?> ) {
					return toInt( ((Return<?>)decoded).value );
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
	
	public static void main(String[] args) throws Exception {
		for( int i=0; i<args.length; ++i ) {
			if( "--version".equals(args[i]) ) {
				System.out.println(NAME+"-v"+VERSION);
			} else if( args[i].startsWith("-") && !"-".equals(args[i]) ) {
				System.err.println("Bad arg: "+args[i]);
				System.exit(1);
			} else {
				String scriptFilePath = args[i++];
				List<String> argv = new ArrayList<String>();
				while( i < args.length ) {
					argv.add(args[i]);
				}
				InputStream inputStream;
				if( "-".equals(scriptFilePath) ) {
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
				
				List<StackyBlockOp<Object,Object>> program = new ArrayList<StackyBlockOp<Object,Object>>();
				program.add(PushOp.of("Hello, world!\n"));
				program.add(EmitOp.instance);
				program.add(PushOp.of(0));
				program.add(ReturnOp.instance);
				
				InterpreterState<Object,Object> interpreterState = new StackyBlockInterpreterState<Object, Object>(
					null,
					new BlockInstructionPointer<>(program, 0),
					new ArrayList<Object>()
				);
				
				SimpleExecutable<Integer> proc = new BabbyInterpreterHarness(br, interpreterState, emitter);
				System.exit(proc.execute().intValue());
			}
		}
	}
}
