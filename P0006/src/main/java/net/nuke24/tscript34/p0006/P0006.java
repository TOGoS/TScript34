package net.nuke24.tscript34.p0006;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class P0006 {
	public static String PSEUDO_OP_BEGIN_PROC = "begin-proc:0:0:0";
	public static String PSEUDO_OP_END_PROC = "end-proc:0:0:0";
	
	// <name>:<op constructor arg count>:<pop count>:<push count>
	public static String OP_PUSH_STRING_1 = "push-string:1"; 
	public static String OP_CONCAT = "concat:0:n:1";
	public static String OP_PRINT = "print:0:1:0";
	public static String OP_QUIT = "quit-process:0:1:never";
	public static String OP_RETURN = "return:0:0:0";
	
	protected static final Pattern dataUriPat = Pattern.compile("^data:,(.*)");
	
	protected static void decodeToken(String token, List<String> dest) {
		Matcher m;
		if( "{".equals(token) ) {
			dest.add(PSEUDO_OP_BEGIN_PROC);
		} else if( "}".equals(token) ) {
			dest.add(PSEUDO_OP_END_PROC);
		} else if( (m = dataUriPat.matcher(token)).matches() ) {
			String data;
			try {
				data = URLDecoder.decode(m.group(1), "utf-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			dest.add(OP_PUSH_STRING_1);
			dest.add(data);
		} else if( "concat".equals(token) ) {
			dest.add(OP_CONCAT);
		} else if( "println".equals(token) ) {
			dest.add(OP_PRINT);
			dest.add(OP_PUSH_STRING_1); dest.add("\n");
			dest.add(OP_PRINT);
		} else {
			throw new RuntimeException("Don't know how to decode token: "+token);
		}
	}
	
	ArrayList<String> program = new ArrayList<String>();
	int ip = 0;
	ArrayList<Object> stack = new ArrayList<Object>();
	int[] returnStack = new int[1024];
	int sp;
	
	public Object pop() {
		return stack.remove(stack.size()-1);
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
		int count = toInt(stack.remove(stack.size()-1));
		int argTop = stack.size();
		int argBottom = argTop-count;
		String result = "";
		int p = argBottom;
		while( p < argTop ) {
			String item = stack.get(p++).toString();
			if( item.length() > 0 ) {
				result = item;
				break;
			}
		}
		while( p < argTop ) {
			String item = stack.get(p++).toString();
			if( item.length() > 0 ) {
				StringBuilder sb = new StringBuilder(result);
				sb.append(item);
				while( p < argTop ) {
					sb.append(stack.get(p++));
				}
				result = sb.toString();
			}
		}
		stack.set(argBottom, result);
		trimTo(stack, argBottom+1);
	}
	
	public void step() {
		String op = program.get(ip++);
		if( op == OP_QUIT ) {
			System.exit(toInt(pop()));
			throw new RuntimeException("Can't get here");
		} else if( op == OP_PUSH_STRING_1 ) {
			stack.add(program.get(ip++));
		} else if( op == OP_CONCAT ) {
			doConcat();
		} else if( op == OP_PRINT ) {
			Object item = stack.remove(stack.size()-1);
			System.out.print(item);
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
		returnStack[sp++] = ip;
	}
	public int popR() {
		return returnStack[--sp];
	}
	
	public void run() {
		while( this.ip >= 0 && ip < program.size() ) step();
	}
	
	public void doToken(String token) {
		int opIndex = program.size();
		decodeToken(token, program);
		String op = program.get(opIndex);
		if( op == PSEUDO_OP_BEGIN_PROC ) {
			++procDepth;
		} else if( op == PSEUDO_OP_END_PROC ) {
			--procDepth;
			if( procDepth < 0 ) {
				throw new RuntimeException("Procedure underflow");
			}
			program.set(opIndex, OP_RETURN);
			return;
		} else if( procDepth == 0 ) {
			this.ip = opIndex;
			run();
		} else {
			return;
		}
		
		// Unless intentionally compiling, in which case should have returned,
		// trim program back to what it was before continuing
		trimTo(program, opIndex);
	}
	
	public static void main(String[] args) {
		P0006 interpreter = new P0006();
		
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
