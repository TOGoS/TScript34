package net.nuke24.tscript34.p0009;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class P0009 {
	// Any Object[] { specialMark, tag, ... } is to be interpreted in some special way, based on tag
	private static final Object SPECIAL_MARK = new Object();
	
	// { SPECIAL_MARK, ST_MARK, ... }
	public static String ST_MARK = "st:mark";
	// [ SPECIAL_MARK, ST_INTRINSIC_OP, opName, ...opArgs }
	public static String ST_INTRINSIC_OP = "st:intrinsic-op";
	// [ SPECIAL_MARK, ST_INTRINSIC_OP_CONSTRUCTOR, opConstructorName }
	public static String ST_INTRINSIC_OP_CONSTRUCTOR = "st:intrinsic-op-constructor";
	// { SPECIAL_MARK, ST_INTRINSIC_OP, ...stuff }
	public static String ST_CONCATENATION = "st:concatenation";

	public static String PSEUDO_OP_BEGIN_PROC = "begin-proc:0:0:0";
	public static String PSEUDO_OP_END_PROC = "end-proc:0:0:0";
	
	public static String OPC_PUSH_VALUE = "http://ns.nuke24.net/TScript34/Op/PushValue";
		
	// <name>:<op constructor arg count>:<pop count>:<push count>
	public static String OP_ARRAY_FROM_STACK = "http://ns.nuke24.net/TScript34/Ops/ArrayFromStack";
	public static String OP_CONCAT = "concat:0:n:1";
	public static String OP_COUNT_TO_MARK = "http://ns.nuke24.net/TScript34/Ops/CountToMark";
	public static String OP_PUSH_LITERAL_1 = "push-literal:1:0:1";
	public static String OP_PUSH_MARK = "http://ns.nuke24.net/TScript34/Ops/PushMark";
	public static String OP_PRINT = "http://ns.nuke24.net/TScript34/Ops/Print";
	public static String OP_PRINT_LINE = "http://ns.nuke24.net/TScript34/Ops/PrintLine";
	public static String OP_PRINT_STACK_THUNKS = "http://ns.nuke24.net/TScript34/Ops/PrintStackThunks";
	public static String OP_QUIT = "http://ns.nuke24.net/TScript34/Ops/Quit";
	public static String OP_QUIT_WITH_CODE = "http://ns.nuke24.net/TScript34/Ops/QuitWithCode";
	public static String OP_RETURN = "return:0:0:0";
	
	protected static final Pattern dataUriPat = Pattern.compile("^data:,(.*)");
	protected static final Pattern decimalIntegerPat = Pattern.compile("^(\\d+)$");
	protected static final Pattern hexIntegerPat = Pattern.compile("^0x([\\da-fA-F]+)$");
	
	public static boolean isSpecial(Object obj) {
		return obj instanceof Object[] && ((Object[])obj).length >= 1 && ((Object[])obj)[0] == SPECIAL_MARK;
	}
	protected static Object[] initSpecial(String tag, int argCount) {
		Object[] special = new Object[argCount+2];
		special[0] = SPECIAL_MARK;
		special[1] = tag;
		return special;
	}
	protected static Object[] mkSpecial(String tag) {
		return initSpecial(tag, 0);
	}
	protected static Object[] mkSpecial(String tag, Object arg) {
		Object[] special = initSpecial(tag, 1);
		special[2] = arg;
		return special;
	}
	protected static Object[] mkSpecial(String tag, Object arg, Object...rest) {
		Object[] special = initSpecial(tag, rest.length+1);
		special[2] = arg;
		for( int i=0; i<rest.length; ++i ) special[i+3] = rest[i];
		return special;
	}
	protected static Object[] mkSpecialFromList(String tag, List<Object> rest) {
		Object[] special = initSpecial(tag, rest.size());
		for( int i=0; i<rest.size(); ++i ) special[i+2] = rest.get(i);
		return special;
	}
	
	public static Object[] mkConcatenation(List<Object> items) {
		return mkSpecialFromList(ST_CONCATENATION, items);
	}

	protected static Object[] mkIntrinsic(Object op0, Object...moreOps) {
		return mkSpecial(ST_INTRINSIC_OP, op0, moreOps);
	}
	
	public static Object[] MARK = mkSpecial(ST_MARK);

	public static void append(Object obj, Appendable dest) throws IOException {
		if( isSpecial(obj) ) {
			Object[] spec = (Object[])obj;
			if( spec[1] == ST_CONCATENATION ) {
				for( int i=2; i<spec.length; ++i ) {
					append(spec[i], dest);
				}
			} else {
				throw new RuntimeException("Don't know how to append "+spec[1]);
			}
		} else {
			dest.append(obj.toString());
		}
	}
	public static String toString(Object obj) {
		if( isSpecial(obj) ) {
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
	public static String toThunkString(Object obj) {
		// TODO: Use same logic as 'append', but with some formatting flag, maybe
		if( obj == SPECIAL_MARK ) {
			return "MARK";
		} else if( obj == SPECIAL_MARK ) {
			return "SPECIAL_MARK";
		} else if( obj instanceof Object[] ) {
			Object[] list = (Object[])obj;
			StringBuilder sb = new StringBuilder("[");
			String sep = "";
			for( int i=0; i<list.length; ++i ) {
				sb.append(sep);
				sb.append(toThunkString(list[i]));
				sep = " ";
			}
			sb.append("]");
			return sb.toString();
		} else if( obj instanceof Integer ) {
			return obj.toString();
		} else {
			return "(" + obj.toString() + ")";
		}
	}
	
	Map<String,Object> definitions = new HashMap<String,Object>();
	
	protected static int decodeToken(String token, Object[] into, int offset) {
		Matcher m;
		if( "{".equals(token) ) {
			into[offset++] = PSEUDO_OP_BEGIN_PROC;
		} else if( "}".equals(token) ) {
			into[offset++] = PSEUDO_OP_END_PROC;
		} else if( (m = decimalIntegerPat.matcher(token)).matches() ) {
			into[offset++] = OP_PUSH_LITERAL_1;
			into[offset++] = Integer.parseInt(m.group(1));
		} else if( (m = hexIntegerPat.matcher(token)).matches() ) {
			into[offset++] = OP_PUSH_LITERAL_1;
			into[offset++] = Integer.parseInt(m.group(1), 16);
		} else if( (m = dataUriPat.matcher(token)).matches() ) {
			String data;
			try {
				data = URLDecoder.decode(m.group(1), "utf-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			into[offset++] = OP_PUSH_LITERAL_1;
			into[offset++] = data;
		} else if( "concat-n".equals(token) ) {
			into[offset++] = OP_CONCAT;
		} else if( "print".equals(token) ) {
			into[offset++] = OP_PRINT;
		} else if( "println".equals(token) ) {
			into[offset++] = OP_PRINT;
			into[offset++] = OP_PUSH_LITERAL_1;
			into[offset++] = "\n";
			into[offset++] = OP_PRINT;
		} else {
			throw new RuntimeException("Don't know how to decode token: "+token);
		}
		return offset;
	}
	
	Object[] program = new Object[1024];
	int programLength = 0;
	int ip = 0;
	Object[] dataStack = new Object[1024];
	int dsp = 0;
	private int[] returnStack = new int[1024];
	private int rsp = 0;
	
	public Object pop() {
		return dataStack[--dsp];
	}
	
	public static int toInt(Object obj) {
		if( obj instanceof Integer ) {
			return ((Integer)obj).intValue();
		} else if( obj instanceof String ) {
			return Integer.parseInt((String)obj);
		} else {
			throw new RuntimeException("Don't know how to get integer from a "+obj.getClass());
		}
	}

	protected boolean isEmpty(Object obj) {
		if( obj instanceof String && ((String)obj).length() == 0 ) return true;
		return false;
	}
	
	public void doConcat() {
		// Overly fancy implementation to avoid creating anything new
		// if only zero or one items are non-empty
		int count = toInt(dataStack[--dsp]);
		int argTop = dsp;
		int argBottom = argTop-count;
		Object result = "";
		int p = argBottom;
		while( p < argTop ) {
			Object item = dataStack[p++];
			if( !isEmpty(item) ) {
				result = item;
				break;
			}
		}
		while( p < argTop ) {
			Object item = dataStack[p++];
			if( !isEmpty(item) ) {
				// If get here, there's at least two things,
				// so we have to concatenate!
				List<Object> catted = new ArrayList<Object>();
				catted.add(result);
				catted.add(item);
				while( p < argTop ) {
					catted.add(dataStack[p++]);
				}
				result = mkConcatenation(catted);
			}
		}
		dataStack[argBottom] = result;
		dsp = argBottom+1;
	}
	
	public void step() {
		Object op = program[ip++];
		if( false ) {
		} else if( op == OP_ARRAY_FROM_STACK ) {
			int count = toInt(dataStack[--dsp]);
			if( count > dsp ) throw new RuntimeException("ArrayFromStack: underflow; can't make "+count+"-length array when only "+dsp+" items on stack!");
			dsp -= count;
			Object[] arr = new Object[count];
			for( int i=0; i<count; ++i ) {
				arr[i] = dataStack[dsp+i];
			}
			dataStack[dsp++] = arr;
		} else if( op == OP_CONCAT ) {
			doConcat();
		} else if( op == OP_COUNT_TO_MARK ) {
			// System.out.println("# CountToMark: dsp="+dsp);
			for( int i=dsp; i-->0; ) {
				if( dataStack[i] == MARK ) {
					int count = dsp-i-1;
					dataStack[dsp++] = Integer.valueOf(count);
					return;
				}
			}
			throw new RuntimeException("CountToMark: No mark found anywhere in stack!");
		} else if( op == OP_PRINT ) {
			if(true) throw new RuntimeException("Who called print?");
			System.out.print(dataStack[--dsp]);
		} else if( op == OP_PUSH_LITERAL_1 ) {
			dataStack[dsp++] = program[ip++];
		} else if( op == OP_PUSH_MARK ) {
			dataStack[dsp++] = MARK;
		} else if( op == OP_QUIT ) {
			System.exit(0);
			throw new RuntimeException("Can't get here");
		} else if( op == OP_QUIT_WITH_CODE ) {
			System.exit(toInt(pop()));
			throw new RuntimeException("Can't get here");
		} else if( op == OP_RETURN ) {
			ip = popR();

		} else if( op == OP_PRINT_STACK_THUNKS ) {
			System.out.println("# Stack ("+dsp+" items):");
			for( int i=dsp; i-->0; ) {
				System.out.println(toThunkString(dataStack[i]));
			}
		} else {
			throw new RuntimeException("Invalid op at index "+(ip-1)+": "+op);
		}
	}
	
	int procDepth = 0;
	
	protected static <T> void trimTo(List<T> list, int size) {
		// Unless compiling, discard the read ops
		while( list.size() > size ) {
			list.remove(list.size()-1);
		}
	}
	
	public void pushR(int ip) {
		if( ++rsp >= returnStack.length ) throw new RuntimeException("Return stack overflow; do less recursion!");
		returnStack[rsp-1] = ip;
	}
	public int popR() {
		if( rsp <= 0 ) throw new RuntimeException("Return stack underflow");
		return returnStack[--rsp];
	}
	
	public void run() {
		while( this.ip >= 0 && ip < programLength ) step();
	}
	
	protected void doDecodedOps(int decodedEnd) {
		final int decodedBegin = programLength;
		Object op = program[decodedBegin];
		if( op == PSEUDO_OP_BEGIN_PROC ) {
			++procDepth;
		} else if( op == PSEUDO_OP_END_PROC ) {
			--procDepth;
			if( procDepth < 0 ) {
				throw new RuntimeException("Procedure underflow");
			}
			program[programLength++] = OP_RETURN;
		} else if( procDepth == 0 ) {
			// Temporarily extend the program to immediately run the decoded ops
			// (this might not be the best way to do things!  Maybe instead there should be
			// special negative instruction pointers to mean 'interpret next token', etc)
			programLength = decodedEnd;
			this.ip = decodedBegin;
			run();
			programLength = decodedBegin;
		} else {
			// Append decoded op to program
			programLength = decodedEnd;
		}
	}
	
	public void doToken(String token) {
		// Append decoded token to program,
		// but leave program length
		final int decodedEnd = decodeToken(token, program, programLength);
		doDecodedOps(decodedEnd);
	}
	
	protected Object get(String name) {
		Object d = definitions.get(name);
		if( d != null ) return d;
		Matcher m;
		if( (m = dataUriPat.matcher(name)).matches() ) {
			try {
				return URLDecoder.decode(m.group(1), "utf-8");
			} catch( UnsupportedEncodingException e ) {
				throw new RuntimeException(e);
			}
		}
		throw new RuntimeException("Don't know about "+name);
	}

	protected int parseTs34_2Op(String[] words, Object[] into, int offset) {
		String opName = words[0];
		Object op = get(opName);
		if( isSpecial(op) ) {
			Object[] opDef = (Object[])op;
			if( opDef[1] == ST_INTRINSIC_OP ) {
				for( int i=2; i<opDef.length; ++i ) {
					into[offset++] = opDef[i];
				}
				return offset;
			} else if( opDef[1] == ST_INTRINSIC_OP_CONSTRUCTOR ) {
				if( opDef[2] == OPC_PUSH_VALUE ) {
					if( words.length < 2 ) {
						throw new RuntimeException(OPC_PUSH_VALUE+" requires at least one argument");
					}
					String name = words[1];
					Object value = get(name);
					if( words.length > 2 ) {
						throw new RuntimeException(OPC_PUSH_VALUE+": encoded values not yet supported!");
					}
					into[offset] = OP_PUSH_LITERAL_1;
					into[offset+1] = toString(value);
					return offset+2;
					//return mkSpecial(ST_INTRINSIC_OP, OP_PUSH_LITERAL_1, toString(value));
				} else {
					throw new RuntimeException("Unrecognized intrinsic op constructor: "+opDef[2]);
				}
			}
		}
		throw new RuntimeException("TODO");
	}
	protected Object parseTs34_2Op(String[] words) {
		int decodedEnd = parseTs34_2Op(words, program, programLength);
		Object[] codes = initSpecial(ST_INTRINSIC_OP, decodedEnd-programLength);
		for( int i=programLength, j=2; i<decodedEnd; ++i, ++j ) {
			codes[j] = program[i];
		}
		return codes;
	}


	public void doTs34_2Line(String line) {
		line = line.trim();
		if( line.length() == 0 || line.startsWith("#") ) return;
		
		String[] words = line.split("\\s+");
		int decodedEnd = parseTs34_2Op(words, program, programLength);
		doDecodedOps(decodedEnd);
	}

	protected static <A,B> Map<A,B> mapOf(A a, B b, Object...rest) {
		Map<A,B> map = new HashMap<A,B>();
		map.put(a, b);
		for( int i=0; i<rest.length; i += 2 ) {
			map.put((A)rest[i], (B)rest[i+1]);
		}
		return map;
	}

	protected static Map<String,?> STANDARD_DEFINITIONS = mapOf(
		OPC_PUSH_VALUE     , mkSpecial(ST_INTRINSIC_OP_CONSTRUCTOR, OPC_PUSH_VALUE),
		OP_ARRAY_FROM_STACK, mkIntrinsic(OP_ARRAY_FROM_STACK),
		OP_COUNT_TO_MARK   , mkIntrinsic(OP_COUNT_TO_MARK),
		OP_PUSH_MARK       , mkIntrinsic(OP_PUSH_MARK),
		OP_PRINT           , mkIntrinsic(OP_PRINT),
		OP_PRINT_LINE      , mkIntrinsic(OP_PRINT, OP_PUSH_LITERAL_1, "\n", OP_PRINT),
		OP_PRINT_STACK_THUNKS, mkIntrinsic(OP_PRINT_STACK_THUNKS),
		OP_QUIT            , mkIntrinsic(OP_QUIT),
		OP_QUIT_WITH_CODE  , mkIntrinsic(OP_QUIT_WITH_CODE)
	);
	
	public static void main(String[] args) throws Exception {
		P0009 interpreter = new P0009();
		interpreter.definitions.putAll(STANDARD_DEFINITIONS);
		
		for( int i=0; i<args.length; ++i ) {
			if( "-t".equals(args[i]) ) {
				for( ++i; i<args.length; ++i ) {
					interpreter.doToken(args[i]);;
				}
			} else if( args[i].startsWith("-") && !"-".equals(args[i]) ) {
				System.err.println("Bad arg: "+args[i]);
				System.exit(1);
			} else {
				String scriptFilePath = args[i++];
				List<String> argv = new ArrayList<String>();
				while( i < args.length ) {
					argv.add(args[i]);
				}
				interpreter.definitions.put("argv", argv);
				interpreter.definitions.put("scriptFilePath", scriptFilePath);
				BufferedReader fr = new BufferedReader("-".equals(scriptFilePath) ? new InputStreamReader(System.in) : new FileReader(scriptFilePath));
				String line;
				int lineIndex = 0;
				while( (line = fr.readLine()) != null ) {
					interpreter.doTs34_2Line(line);
					++lineIndex;
				}
			}
		}
	}
}
