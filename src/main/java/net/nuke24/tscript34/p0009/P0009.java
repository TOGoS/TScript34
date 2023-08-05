package net.nuke24.tscript34.p0009;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class P0009 {
	public static String PSEUDO_OP_BEGIN_PROC = "begin-proc:0:0:0";
	public static String PSEUDO_OP_END_PROC = "end-proc:0:0:0";
	
	// <name>:<op constructor arg count>:<pop count>:<push count>
	public static String OP_PUSH_LITERAL_1 = "push-literal:1:0:1";
	public static String OP_CONCAT = "concat:0:n:1";
	public static String OP_PRINT = "print:0:1:0";
	public static String OP_QUIT = "quit-process:0:1:never";
	public static String OP_RETURN = "return:0:0:0";
	
	protected static final Pattern dataUriPat = Pattern.compile("^data:,(.*)");
	protected static final Pattern decimalIntegerPat = Pattern.compile("^(\\d+)$");
	protected static final Pattern hexIntegerPat = Pattern.compile("^0x([\\da-fA-F]+)$");
	
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
	
	public void doConcat() {
		// Overly fancy implementation to avoid creating anything new
		// if only zero or one items are non-empty
		int count = toInt(dataStack[--dsp]);
		int argTop = dsp;
		int argBottom = argTop-count;
		String result = "";
		int p = argBottom;
		while( p < argTop ) {
			String item = dataStack[p++].toString();
			if( item.length() > 0 ) {
				result = item;
				break;
			}
		}
		while( p < argTop ) {
			String item = dataStack[p++].toString();
			if( item.length() > 0 ) {
				StringBuilder sb = new StringBuilder(result);
				sb.append(item);
				while( p < argTop ) {
					sb.append(dataStack[p++]);
				}
				result = sb.toString();
			}
		}
		dataStack[argBottom] = result;
		dsp = argBottom+1;
	}
	
	public void step() {
		Object op = program[ip++];
		if( op == OP_QUIT ) {
			System.exit(toInt(pop()));
			throw new RuntimeException("Can't get here");
		} else if( op == OP_PUSH_LITERAL_1 ) {
			dataStack[dsp++] = program[ip++];
		} else if( op == OP_CONCAT ) {
			doConcat();
		} else if( op == OP_PRINT ) {
			System.out.print(dataStack[--dsp]);
		} else if( op == OP_RETURN ) {
			ip = popR();
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
	
	public void doToken(String token) {
		// Append decoded token to program,
		// but leave program length
		final int decodedBegin = programLength;
		final int decodedEnd = decodeToken(token, program, decodedBegin);
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
	
	public static void main(String[] args) {
		P0009 interpreter = new P0009();
		
		for( int i=0; i<args.length; ++i ) {
			if( "-t".equals(args[i]) ) {
				for( ++i; i<args.length; ++i ) {
					interpreter.doToken(args[i]);;
				}
			} else {
				System.err.println("Bad arg: "+args[i]);
				System.exit(1);
			}
		}
	}
}
