package net.nuke24.tscript34.p0019;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class P0019 {
	public static String NAME = "TS34.19";
	public static String VERSION = "0.0.3";

	// Any Object[] { specialMark, tag, ... } is to be interpreted in some special way, based on tag
	public static final Object SPECIAL_MARK = new Object();
	
	// 'HOC_' are 'high ops'; those that live in the high bits,
	// using the lower bits as parameters
	static final int HOC_VAL_MASK  = 0x00FFFFFF;
	static final int HOC_OP_MASK   = 0xFF000000;
	// Lower bits are literal value to push (positive int25s)
	static final int HOC_PUSH_PINT = 0x00000000;
	// Lower bits are literal value to push (negative int25s)
	static final int HOC_PUSH_NINT = 0xFF000000;
	// Lower bits are index into constant object table
	static final int HOC_PUSH_OBJ  = 0x01000000;
	static final int HOC_JUMP      = 0x02000000;
	
	// Individual opcodes in no particular order,
	// and don't rely on these IDs staying constant
	static final int OC_RETURN           = 0x80000001;
	static final int OC_CONCAT_N         = 0x80000002;
	static final int OC_ARRAY_FROM_STACK = 0x80000003;
	static final int OC_COUNT_TO_MARK    = 0x80000004;
	static final int OC_DUP              = 0x80000005;
	static final int OC_EXCH             = 0x80000006;
	static final int OC_EXECUTE          = 0x80000007;
	static final int OC_JUMP             = 0x80000008;
	static final int OC_POP              = 0x80000009;
	static final int OC_PRINT            = 0x8000000A;
	static final int OC_PUSH_MARK        = 0x8000000B;
	static final int OC_GET_INTERPRETER_INFO = 0x8000000C;
	static final int OC_PRINT_STACK_THUNKS = 0x8000000D;
	static final int OC_QUIT             = 0x8000000E;
	static final int OC_QUIT_WITH_CODE   = 0x8000000F;
	
	// Fake opcodes, for decoding
	static final int HOC_FAKE            = 0x90000000;
	static final int OC_OPEN_PROC        = 0x90000001;
	static final int OC_CLOSE_PROC       = 0x90000002;
	
	public static int mkOc(int hoc, int value) {
		return (hoc&HOC_OP_MASK) | (value&HOC_VAL_MASK);
	}
	public static int mkLiteralIntOc(int value) {
		switch( value & HOC_OP_MASK ) {
		case HOC_PUSH_PINT: case HOC_PUSH_NINT:
			return value;
		default:
			throw new RuntimeException("Can't encode integer "+value+" ("+formatOc(value)+" in a single op; too large");
		}
	}
	
	// { SPECIAL_MARK, ST_MARK, ... }
	public static String ST_MARK = "st:mark";
	// [ SPECIAL_MARK, ST_INTRINSIC_OP, opName, ...opArgs }
	public static String ST_INTRINSIC_OP = "st:intrinsic-op";
	// [ SPECIAL_MARK, ST_INTRINSIC_OP_CONSTRUCTOR, opConstructorName }
	public static String ST_INTRINSIC_OP_CONSTRUCTOR = "st:intrinsic-op-constructor";
	// { SPECIAL_MARK, ST_INTRINSIC_OP, ...stuff }
	public static String ST_CONCATENATION = "st:concatenation";
	// { SPECIAL_MARK, ST_PROCEDURE_ADDRESS, indexOfFirstOpOfProcedure }
	public static String ST_PROCEDURE_ADDRESS = "st:procedure-address";

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
	public static String OP_PUSH_LITERAL_1 = "push-literal:1:0:1";
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

	public static Object[] mkIntrinsic(int...ops) {
		Object[] special = initSpecial(ST_INTRINSIC_OP, ops.length);
		for( int i=0; i<ops.length; ++i ) special[i+2] = Integer.valueOf(ops[i]);
		return special;
	}
	public static Object[] mkProcByAddress(int address) {
		return mkSpecial(ST_PROCEDURE_ADDRESS, address);
	}
	
	public static Object[] MARK = mkSpecial(ST_MARK);

	public static String formatOc(int opCode) {
		return "0x"+Integer.toUnsignedString(opCode, 16);
	}
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
	
	int[] program = new int[1024];
	int programLength = 0;
	int ip = 0;
	Object[] dataStack = new Object[1024];
	int dsp = 0;
	private int[] returnStack = new int[1024];
	private int rsp = 0;
	private Object[] constants = new Object[1024];
	private int constantCount = 0;
	
	public int findConstant(Object obj) {
		for( int i=0; i<constantCount; ++i ) {
			if( constants[i].equals(obj) ) {
				return i;
			}
		}
		throw new RuntimeException("Constant not found: "+toThunkString(obj));
	}
	protected int addConstant(Object obj) {
		int index = constantCount++;
		constants[index] = obj;
		return index;
	}
	protected int addConstant(Object obj, int assertedIndex) {
		int actualIndex = addConstant(obj);
		if( actualIndex != assertedIndex ) {
			throw new RuntimeException("Expected new constant to be at index "+assertedIndex+", but would be at "+actualIndex);
		}
		return actualIndex;
	}
	
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
		int opCode = program[ip++];
		switch( opCode & HOC_OP_MASK ) {
		case HOC_PUSH_PINT: case HOC_PUSH_NINT:
			dataStack[dsp++] = Integer.valueOf(opCode);
			return;
		case HOC_PUSH_OBJ:
			dataStack[dsp++] = constants[opCode & HOC_VAL_MASK];
			return;
		case HOC_JUMP:
			ip = opCode & HOC_VAL_MASK;
			return;
		}
		
		int count;
		
		// Regular ops:
		switch( opCode ) {
		case OC_ARRAY_FROM_STACK:
			count = toInt(dataStack[--dsp]);
			if( count > dsp ) throw new RuntimeException("ArrayFromStack: underflow; can't make "+count+"-length array when only "+dsp+" items on stack!");
			dsp -= count;
			Object[] arr = new Object[count];
			for( int i=0; i<count; ++i ) {
				arr[i] = dataStack[dsp+i];
			}
			dataStack[dsp++] = arr;
			return;
		case OC_CONCAT_N:
			doConcat();
			return;
		case OC_COUNT_TO_MARK:
			// System.out.println("# CountToMark: dsp="+dsp);
			for( int i=dsp; i-->0; ) {
				if( dataStack[i] == MARK ) {
					count = dsp-i-1;
					dataStack[dsp++] = Integer.valueOf(count);
					return;
				}
			}
			throw new RuntimeException("CountToMark: No mark found anywhere in stack!");
		case OC_DUP:
			dataStack[dsp] = dataStack[dsp-1];
			++dsp;
			return;
		case OC_EXCH:
			Object top = dataStack[dsp-1];
			dataStack[dsp-1] = dataStack[dsp-2];
			dataStack[dsp-2] = top;
			return;
		case OC_EXECUTE:
			Object thing = dataStack[--dsp];
			if( isSpecial(thing) && ((Object[])thing)[1] == ST_PROCEDURE_ADDRESS ) {
				pushR(ip);
				ip = toInt(((Object[])thing)[2]);
			} else {
				throw new RuntimeException("Don't know how to execute "+toString(thing));
			}
			return;
		case OC_JUMP:
			ip = toInt(dataStack[--dsp]);
			return;
		case OC_POP:
			--dsp;
			return;
		case OC_PRINT:
			System.out.print(toString(dataStack[--dsp]));
			return;
		case OC_PUSH_MARK:
			dataStack[dsp++] = MARK;
			return;
		case OC_QUIT:
			System.exit(0);
			return;
		case OC_QUIT_WITH_CODE:
			System.exit(toInt(pop()));
			throw new RuntimeException("Can't get here");
		case OC_RETURN:
			ip = popR();
			return;

		case OC_GET_INTERPRETER_INFO:
			dataStack[dsp++] = NAME+"-v"+VERSION;
		case OC_PRINT_STACK_THUNKS:
			System.out.println("# Stack ("+dsp+" items):");
			for( int i=dsp; i-->0; ) {
				System.out.println(toThunkString(dataStack[i]));
			}
		default:
			throw new RuntimeException("Invalid op at index "+(ip-1)+": "+opCode);
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
		int opCode = program[decodedBegin];
		if( (opCode & HOC_OP_MASK) == HOC_FAKE ) switch( opCode ) {
		case OC_OPEN_PROC:
			if( procDepth > 0 ) {
				pushR(programLength++);
			}
			pushR(programLength);
			++procDepth;
			return;
		case OC_CLOSE_PROC:
			Object proc = mkProcByAddress(popR());
			--procDepth;
			if( procDepth < 0 ) {
				throw new RuntimeException("Procedure underflow");
			}
			program[programLength++] = OC_RETURN;
			if( procDepth > 0 ) {
				int procObjId = addConstant(proc);
				int fixupJumpIndex = popR();
				program[fixupJumpIndex] = mkOc(HOC_JUMP, programLength);
				program[programLength++] = mkOc(HOC_PUSH_OBJ, procObjId);
			} else {
				dataStack[dsp++] = proc;
			}
			return;
		default:
			throw new RuntimeException("Unrecognized fake opcode: 0x"+Integer.toUnsignedString(opCode));
		}
		
		if( procDepth == 0 ) {
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

	protected int parseTs34Op(String[] words, int[] into, int offset) {
		String opName = words[0];
		Object op = get(opName);
		if( isSpecial(op) ) {
			Object[] opDef = (Object[])op;
			if( opDef[1] == ST_INTRINSIC_OP ) {
				if( words.length > 1 ) {
					throw new RuntimeException("Excessive arguments to op "+opName);
				}
				for( int i=2; i<opDef.length; ++i ) {
					if( !(opDef[i] instanceof Integer) ) {
						throw new RuntimeException("Elements after "+ST_INTRINSIC_OP+" should be integers; found a "+opDef[i].getClass()+" at iundex "+i);
					}
					int opCode = toInt(opDef[i]);
					into[offset++] = opCode;
				}
				return offset;
			} else if( opDef[1] == ST_INTRINSIC_OP_CONSTRUCTOR ) {
				if( opDef[2] == OPC_ALIAS ) {
					if( words.length != 3 ) {
						throw new RuntimeException(OPC_ALIAS+" requires exactly two arguments");
					}
					definitions.put(words[1], get(words[2]));
					return offset;
				} else if( opDef[2] == OPC_PUSH_VALUE ) {
					if( words.length < 2 ) {
						throw new RuntimeException(OPC_PUSH_VALUE+" requires at least one argument");
					}
					String name = words[1];
					Object value = get(name);
					// TODO: resolve the encoding URIs
					if( words.length == 3 && P0019.DATATYPE_DECIMAL.equals(words[2]) ) {
						value = Integer.valueOf(value.toString());
					} else if( words.length > 2 ) {
						throw new RuntimeException(OPC_PUSH_VALUE+": arbitrarily encoded values not yet supported!");
					}
					if( value instanceof Integer ) {
						into[offset++] = mkLiteralIntOc((Integer)value);
					} else {
						into[offset++] = mkOc(HOC_PUSH_OBJ, addConstant(value));
					}
					return offset;
					//return mkSpecial(ST_INTRINSIC_OP, OP_PUSH_LITERAL_1, toString(value));
				} else {
					throw new RuntimeException("Unrecognized intrinsic op constructor: "+opDef[2]);
				}
			}
		}
		throw new RuntimeException("TODO");
	}
	protected Object parseTs34Op(String[] words) {
		int decodedEnd = parseTs34Op(words, program, programLength);
		Object[] codes = initSpecial(ST_INTRINSIC_OP, decodedEnd-programLength);
		for( int i=programLength, j=2; i<decodedEnd; ++i, ++j ) {
			codes[j] = program[i];
		}
		return codes;
	}

	public void doTs34Line(String...words) {
		int decodedEnd = parseTs34Op(words, program, programLength);
		doDecodedOps(decodedEnd);
	}

	public void doTs34Line(String line) {
		line = line.trim();
		if( line.length() == 0 || line.startsWith("#") ) return;
		
		String[] words = line.split("\\s+");
		doTs34Line(words);
	}

	@SuppressWarnings("unchecked")
	protected static <A,B> Map<A,B> mapOf(A a, B b, Object...rest) {
		Map<A,B> map = new HashMap<A,B>();
		map.put(a, b);
		for( int i=0; i<rest.length; i += 2 ) {
			map.put((A)rest[i], (B)rest[i+1]);
		}
		return map;
	}

	static final int CONST_INDEX_MARK = 0;
	static final int CONST_INDEX_NEWLINE = 1;
	
	protected static Map<String,?> STANDARD_DEFINITIONS = mapOf(
		OPC_ALIAS          , mkSpecial(ST_INTRINSIC_OP_CONSTRUCTOR, OPC_ALIAS),
		OPC_PUSH_VALUE     , mkSpecial(ST_INTRINSIC_OP_CONSTRUCTOR, OPC_PUSH_VALUE),
		OP_ARRAY_FROM_STACK, mkIntrinsic(OC_ARRAY_FROM_STACK),
		OP_CONCAT_N        , mkIntrinsic(OC_CONCAT_N),
		OP_COUNT_TO_MARK   , mkIntrinsic(OC_COUNT_TO_MARK),
		OP_CLOSE_PROC      , mkIntrinsic(OC_CLOSE_PROC),
		OP_DUP             , mkIntrinsic(OC_DUP),
		OP_EXCH            , mkIntrinsic(OC_EXCH),
		OP_EXECUTE         , mkIntrinsic(OC_EXECUTE),
		OP_GET_INTERPRETER_INFO, mkIntrinsic(OC_GET_INTERPRETER_INFO),
		OP_OPEN_PROC       , mkIntrinsic(OC_OPEN_PROC),
		OP_POP             , mkIntrinsic(OC_POP),
		OP_PUSH_MARK       , mkIntrinsic(mkOc(HOC_PUSH_OBJ, CONST_INDEX_MARK)),
		OP_PRINT           , mkIntrinsic(OC_PRINT),
		OP_PRINT_LINE      , mkIntrinsic(OC_PRINT, mkOc(HOC_PUSH_OBJ, CONST_INDEX_NEWLINE), OC_PRINT),
		OP_PRINT_STACK_THUNKS, mkIntrinsic(OC_PRINT_STACK_THUNKS),
		OP_QUIT            , mkIntrinsic(OC_QUIT),
		OP_QUIT_WITH_CODE  , mkIntrinsic(OC_QUIT_WITH_CODE)
	);
	
	public P0019() {
		// Const 0 - the mark
		addConstant(MARK, CONST_INDEX_MARK);
		// Const 1 - newline string
		addConstant("\n", CONST_INDEX_NEWLINE);
	}
	
	public static void main(String[] args) throws Exception {
		P0019 interpreter = new P0019();
		interpreter.definitions.putAll(STANDARD_DEFINITIONS);
		
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
				interpreter.definitions.put("argv", argv);
				interpreter.definitions.put("scriptFilePath", scriptFilePath);
				BufferedReader fr = new BufferedReader("-".equals(scriptFilePath) ? new InputStreamReader(System.in) : new FileReader(scriptFilePath));
				String line;
				@SuppressWarnings("unused")
				int lineIndex = 0;
				while( (line = fr.readLine()) != null ) {
					interpreter.doTs34Line(line);
					++lineIndex;
				}
			}
		}
	}
}
