package net.nuke24.tscript34.p0009;

import java.io.IOException;
import java.lang.Appendable;
import java.util.ArrayList;
import java.util.Arrays;

public class P0009Test {
	protected static boolean equals(Object a, Object b) {
		if( a == null && b == null ) return true;
		if( a == null || b == null ) {
			return false;
		}
				
		if( a instanceof String && P0009.isSpecial(b) ) {
			b = P0009.toString(b);
		}
		
		if( (a instanceof Object[]) && (b instanceof Object[]) ) {
			assertEqualsArr((Object[])a, (Object[])b);
		} else {
			if( !a.equals(b) ) {
				return false;
			}
		}
		
		return true;
	}
	
	protected static void assertEquals(Object a, Object b) {
		if( !equals(a,b) ) throw new RuntimeException("assertEquals fails: "+a+" != "+b);
	}
	
	protected static <T> void toString(T[] arr, int off, int len, Appendable dest) throws IOException {
		dest.append("[");
		String sep = "";
		for( int i=0; i<len; ++i ) {
			dest.append(sep);
			toString( arr[i], dest );
			sep = ", ";
		}
		dest.append("]");
	}
	protected static void toString(Object obj, Appendable dest) throws IOException {
		if( obj == null ) {
			dest.append("null");
		} else if( obj == P0009.SPECIAL_MARK ) {
			dest.append("SPECIAL_MARK");
		} else if( obj instanceof Object[] ) {
			toString((Object[])obj, 0, ((Object[])obj).length, dest);
		} else if( obj instanceof String ) {
			dest.append("\"" + obj.toString().replace("\\","\\\\").replace("\"","\\\"") + "\"");
		} else {
			dest.append(obj.toString());
		}
	}
	protected static String toString(Object obj) {
		StringBuilder sb = new StringBuilder();
		try { toString(obj, sb); } catch( IOException e ) { }
		return sb.toString();
	}
	
	protected static <T> void assertSubArrayEquals(T[] expected, int offE, int lenE, T[] actual, int offA, int lenA) {
		ArrayList<String> failures = new ArrayList<String>();
		if( lenE != lenA ) {
			failures.add("length: "+lenE+" != "+lenA);
		}
		int checkLen = lenE < lenA ? lenE : lenA;
		for( int i=0; i<checkLen; ++i ) {
			if( !equals(expected[offE+i], actual[offA+i]) ) {
				failures.add("["+i+"]: "+toString(expected[offE+i])+" != "+toString(actual[offA+i]));
			}
		}
		if( failures.size() > 0 ) try {
			StringBuilder sb = new StringBuilder();
			sb.append("Arrays not equal:");
			//for( String failure : failures ) {
			//	sb.append("\n- "+failure);
			//}
			toString(expected, offE, lenE, sb);
			sb.append(" != ");
			toString(actual, offA, lenA, sb);
			throw new RuntimeException(sb.toString());
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	protected static <T> void assertEqualsArr(T[] a, T[] b) {
		assertSubArrayEquals(a, 0, a.length, b, 0, b.length);
	}
	
	protected void assertProgramEquals(Object[] expected, P0009 interp) {
		assertSubArrayEquals(
			expected, 0, expected.length,
			interp.program, 0, interp.programLength
		);
	}
	
	protected void assertDataStackEquals(Object[] expected, P0009 interp) {
		assertSubArrayEquals(
			expected, 0, expected.length,
			interp.dataStack, 0, interp.dsp
		);
	}

	public void testConcat() {
		P0009 interp = new P0009();
		interp.doToken("data:,foo");
		interp.doToken("data:,bar");
		interp.doToken("2");
		interp.doToken("concat-n");
		
		assertSubArrayEquals(
			new Object[] { "foobar" }, 0, 1,
			interp.dataStack, 0, 1
		);
	}
	
	public void testCompileDecimalNumber() {
		P0009 interp = new P0009();
		interp.doToken("{");
		interp.doToken("1025");
		interp.doToken("}");
		assertProgramEquals(
			new Object[] { P0009.OP_PUSH_LITERAL_1, Integer.valueOf(1025), P0009.OP_RETURN },
			interp
		);
		interp.pop();
		interp.pushR(-1);
		interp.run();
		assertDataStackEquals(new Object[] { Integer.valueOf(1025) }, interp);
	}

	protected P0009 mkInterp() {
		P0009 interp = new P0009();
		interp.definitions.putAll(P0009.STANDARD_DEFINITIONS);
		return interp;
	}
	
	public void testParsePushValue() {
		P0009 interp = mkInterp();
		Object parsed = interp.parseTs34Op(new String[] { P0009.OPC_PUSH_VALUE, "data:,hi%20there" });
		assertEquals(parsed, P0009.mkSpecial(P0009.ST_INTRINSIC_OP, P0009.OP_PUSH_LITERAL_1, "hi there"));
	}
	
	public void testDoPushValue() {
		P0009 interp = mkInterp();
		interp.doTs34Line(P0009.OPC_PUSH_VALUE+" data:,hi%20there");
		assertSubArrayEquals(
			new Object[] { "hi there" }, 0, 1,
			interp.dataStack, 0, 1
		);
	}

	public void testMakeArray() {
		P0009 interp = mkInterp();
		interp.doTs34Line(P0009.OP_PUSH_MARK);
		interp.doTs34Line(P0009.OPC_PUSH_VALUE+" data:,abc");
		interp.doTs34Line(P0009.OPC_PUSH_VALUE+" data:,def");
		interp.doTs34Line(P0009.OPC_PUSH_VALUE+" data:,ghi");
		interp.doTs34Line(P0009.OP_COUNT_TO_MARK);
		//interp.doTs34Line(P0009.OP_PRINT_STACK_THUNKS);
		interp.doTs34Line(P0009.OP_ARRAY_FROM_STACK);
		//interp.doTs34Line(P0009.OP_PRINT_STACK_THUNKS);
		assertSubArrayEquals(
			new Object[] { new Object[] { "abc","def","ghi" } }, 0, 1,
			interp.dataStack, 1, 1 // Mark's still under it!
		);
	}
	
	public void testDup() {
		P0009 interp = mkInterp();
		interp.doTs34Line(P0009.OPC_PUSH_VALUE+" data:,abc");
		interp.doTs34Line(P0009.OPC_PUSH_VALUE+" data:,def");
		interp.doTs34Line(P0009.OP_DUP);
		assertSubArrayEquals(
			new Object[] { "abc","def","def" }, 0, 3,
			interp.dataStack, 0, interp.dsp
		);
	}
	
	public void testPop() {
		P0009 interp = mkInterp();
		interp.doTs34Line(P0009.OPC_PUSH_VALUE+" data:,abc");
		interp.doTs34Line(P0009.OPC_PUSH_VALUE+" data:,def");
		interp.doTs34Line(P0009.OPC_PUSH_VALUE+" data:,ghi");
		interp.doTs34Line(P0009.OP_POP);
		assertSubArrayEquals(
			new Object[] { "abc","def" }, 0, 2,
			interp.dataStack, 0, interp.dsp
		);
	}
	
	public void testExch() {
		P0009 interp = mkInterp();
		interp.doTs34Line(P0009.OPC_PUSH_VALUE,"data:,abc");
		interp.doTs34Line(P0009.OPC_PUSH_VALUE,"data:,def");
		interp.doTs34Line(P0009.OPC_PUSH_VALUE,"data:,ghi");
		interp.doTs34Line(P0009.OP_EXCH);
		assertSubArrayEquals(
			new Object[] { "abc", "ghi", "def" }, 0, 3,
			interp.dataStack, 0, interp.dsp
		);
	}
	
	public void testAlias() {
		P0009 interp = mkInterp();
		interp.doTs34Line(P0009.OPC_ALIAS,"push",P0009.OPC_PUSH_VALUE);
		interp.doTs34Line("push","data:,abc");
		assertDataStackEquals(new Object[] { "abc" }, interp);
	}
	
	public void testProc() {
		P0009 interp = mkInterp();
		interp.doTs34Line(P0009.OP_OPEN_PROC);
		interp.doTs34Line(P0009.OP_CLOSE_PROC);
		assertProgramEquals(new Object[] { P0009.OP_RETURN }, interp);
		assertDataStackEquals(new Object[] { P0009.mkProcByAddress(0) }, interp);
	}
	
	public void testMoreProc() {
		P0009 interp = mkInterp();
		interp.doTs34Line(P0009.OP_OPEN_PROC);
		interp.doTs34Line(P0009.OPC_PUSH_VALUE,"data:,Henry");
		interp.doTs34Line(P0009.OP_CLOSE_PROC);
		assertProgramEquals(new Object[] { P0009.OP_PUSH_LITERAL_1, "Henry", P0009.OP_RETURN }, interp);
		assertDataStackEquals(new Object[] { P0009.mkProcByAddress(0) }, interp);

		interp.doTs34Line(P0009.OP_EXECUTE);
		assertDataStackEquals(new Object[] { "Henry" }, interp);
	}
	public void testNestedProc() {
		P0009 interp = mkInterp();
		interp.doTs34Line(P0009.OP_OPEN_PROC);
		int outerProcAddress = interp.programLength;
		interp.doTs34Line(P0009.OP_OPEN_PROC);
		int innerProcAddress = interp.programLength;
		interp.doTs34Line(P0009.OPC_PUSH_VALUE,"data:,Henry");
		interp.doTs34Line(P0009.OP_CLOSE_PROC);
		interp.doTs34Line(P0009.OP_CLOSE_PROC);
		assertDataStackEquals(new Object[] { P0009.mkProcByAddress(outerProcAddress) }, interp);

		interp.doTs34Line(P0009.OP_EXECUTE);
		assertDataStackEquals(new Object[] { P0009.mkProcByAddress(innerProcAddress) }, interp);

		interp.doTs34Line(P0009.OP_EXECUTE);
		assertDataStackEquals(new Object[] { "Henry" }, interp);
	}
	
	public void run() {
		testConcat();
		testCompileDecimalNumber();
		testParsePushValue();
		testDoPushValue();
		testMakeArray();
		testDup();
		testPop();
		testExch();
		testAlias();
		testProc();
		testMoreProc();
		testNestedProc();
	}
	
	public static void main(String[] args) {
		new P0009Test().run();
	}
}
