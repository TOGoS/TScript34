package net.nuke24.tscript34.p0019;

import java.io.IOException;
import java.util.ArrayList;

import net.nuke24.tscript34.p0019.P0019;

public class P0019Test {
	protected static boolean equals(Object a, Object b) {
		if( a == null && b == null ) return true;
		if( a == null || b == null ) {
			return false;
		}
				
		if( a instanceof String && P0019.isSpecial(b) ) {
			b = P0019.toString(b);
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
	
	protected static void toString(int[] arr, int off, int len, Appendable dest) throws IOException {
		dest.append("[");
		String sep = "";
		for( int i=0; i<len; ++i ) {
			dest.append(sep);
			dest.append("0x");
			dest.append(Integer.toString(arr[i], 16));
			sep = ", ";
		}
		dest.append("]");
	}
	protected static <T> void toString(T[] arr, int off, int len, Appendable dest) throws IOException {
		dest.append("[");
		String sep = "";
		for( int i=0; i<len; ++i ) {
			dest.append(sep);
			toString( arr[off+i], dest );
			sep = ", ";
		}
		dest.append("]");
	}
	protected static void toString(Object obj, Appendable dest) throws IOException {
		if( obj == null ) {
			dest.append("null");
		} else if( obj == P0019.SPECIAL_MARK ) {
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
	
	protected static <T> void assertSubArrayEquals(int[] expected, int offE, int lenE, int[] actual, int offA, int lenA) {
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
	
	protected void assertProgramEquals(int[] expected, P0019 interp) {
		assertSubArrayEquals(
			expected, 0, expected.length,
			interp.program, 0, interp.programLength
		);
	}
	
	protected void assertDataStackEquals(Object[] expected, P0019 interp) {
		assertSubArrayEquals(
			expected, 0, expected.length,
			interp.dataStack, 0, interp.dsp
		);
	}

	public void testConcat() {
		P0019 interp = mkInterp();
		interp.doTs34Line(P0019.OP_PUSH_MARK);
		interp.doTs34Line(P0019.OPC_PUSH_VALUE, "data:,foo");
		interp.doTs34Line(P0019.OPC_PUSH_VALUE, "data:,bar");
		interp.doTs34Line(P0019.OP_COUNT_TO_MARK);
		interp.doTs34Line(P0019.OP_CONCAT_N);
		
		assertSubArrayEquals(
			new Object[] { "foobar" }, 0, 1,
			interp.dataStack, 1, 1
		);
	}
	
	public void testCompileDecimalNumber() {
		P0019 interp = mkInterp();
		interp.doTs34Line(P0019.OP_OPEN_PROC);
		interp.doTs34Line(P0019.OPC_PUSH_VALUE, "data:,1025", P0019.DATATYPE_DECIMAL);
		interp.doTs34Line(P0019.OP_CLOSE_PROC);
		assertProgramEquals(
			new int[] { P0019.mkLiteralIntOc(1025), P0019.OC_RETURN },
			interp
		);
		interp.pop();
		interp.pushR(-1);
		interp.run();
		assertDataStackEquals(new Object[] { Integer.valueOf(1025) }, interp);
	}

	protected P0019 mkInterp() {
		P0019 interp = new P0019();
		interp.definitions.putAll(P0019.STANDARD_DEFINITIONS);
		return interp;
	}
	
	public void testParsePushValue() {
		P0019 interp = mkInterp();
		Object parsed = interp.parseTs34Op(new String[] { P0019.OPC_PUSH_VALUE, "data:,hi%20there" });
		assertEquals(
			P0019.mkSpecial(P0019.ST_INTRINSIC_OP, P0019.mkOc(P0019.HOC_PUSH_OBJ, interp.findConstant("hi there"))),
			parsed
		);
	}
	
	public void testDoPushValue() {
		P0019 interp = mkInterp();
		interp.doTs34Line(P0019.OPC_PUSH_VALUE+" data:,hi%20there");
		assertSubArrayEquals(
			new Object[] { "hi there" }, 0, 1,
			interp.dataStack, 0, 1
		);
	}

	public void testMakeArray() {
		P0019 interp = mkInterp();
		interp.doTs34Line(P0019.OP_PUSH_MARK);
		interp.doTs34Line(P0019.OPC_PUSH_VALUE+" data:,abc");
		interp.doTs34Line(P0019.OPC_PUSH_VALUE+" data:,def");
		interp.doTs34Line(P0019.OPC_PUSH_VALUE+" data:,ghi");
		interp.doTs34Line(P0019.OP_COUNT_TO_MARK);
		//interp.doTs34Line(P0009.OP_PRINT_STACK_THUNKS);
		interp.doTs34Line(P0019.OP_ARRAY_FROM_STACK);
		//interp.doTs34Line(P0009.OP_PRINT_STACK_THUNKS);
		assertSubArrayEquals(
			new Object[] { new Object[] { "abc","def","ghi" } }, 0, 1,
			interp.dataStack, 1, 1 // Mark's still under it!
		);
	}
	
	public void testDup() {
		P0019 interp = mkInterp();
		interp.doTs34Line(P0019.OPC_PUSH_VALUE+" data:,abc");
		interp.doTs34Line(P0019.OPC_PUSH_VALUE+" data:,def");
		interp.doTs34Line(P0019.OP_DUP);
		assertSubArrayEquals(
			new Object[] { "abc","def","def" }, 0, 3,
			interp.dataStack, 0, interp.dsp
		);
	}
	
	public void testPop() {
		P0019 interp = mkInterp();
		interp.doTs34Line(P0019.OPC_PUSH_VALUE+" data:,abc");
		interp.doTs34Line(P0019.OPC_PUSH_VALUE+" data:,def");
		interp.doTs34Line(P0019.OPC_PUSH_VALUE+" data:,ghi");
		interp.doTs34Line(P0019.OP_POP);
		assertSubArrayEquals(
			new Object[] { "abc","def" }, 0, 2,
			interp.dataStack, 0, interp.dsp
		);
	}
	
	public void testExch() {
		P0019 interp = mkInterp();
		interp.doTs34Line(P0019.OPC_PUSH_VALUE,"data:,abc");
		interp.doTs34Line(P0019.OPC_PUSH_VALUE,"data:,def");
		interp.doTs34Line(P0019.OPC_PUSH_VALUE,"data:,ghi");
		interp.doTs34Line(P0019.OP_EXCH);
		assertSubArrayEquals(
			new Object[] { "abc", "ghi", "def" }, 0, 3,
			interp.dataStack, 0, interp.dsp
		);
	}
	
	public void testAlias() {
		P0019 interp = mkInterp();
		interp.doTs34Line(P0019.OPC_ALIAS,"push",P0019.OPC_PUSH_VALUE);
		interp.doTs34Line("push","data:,abc");
		assertDataStackEquals(new Object[] { "abc" }, interp);
	}
	
	public void testProc() {
		P0019 interp = mkInterp();
		interp.doTs34Line(P0019.OP_OPEN_PROC);
		interp.doTs34Line(P0019.OP_CLOSE_PROC);
		assertProgramEquals(new int[] { P0019.OC_RETURN }, interp);
		assertDataStackEquals(new Object[] { P0019.mkProcByAddress(0) }, interp);
	}
	
	public void testMoreProc() {
		P0019 interp = mkInterp();
		interp.doTs34Line(P0019.OP_OPEN_PROC);
		interp.doTs34Line(P0019.OPC_PUSH_VALUE,"data:,Henry");
		interp.doTs34Line(P0019.OP_CLOSE_PROC);
		assertProgramEquals(new int[] { P0019.mkOc(P0019.HOC_PUSH_OBJ, interp.findConstant("Henry")), P0019.OC_RETURN }, interp);
		assertDataStackEquals(new Object[] { P0019.mkProcByAddress(0) }, interp);

		interp.doTs34Line(P0019.OP_EXECUTE);
		assertDataStackEquals(new Object[] { "Henry" }, interp);
	}
	public void testNestedProc() {
		P0019 interp = mkInterp();
		interp.doTs34Line(P0019.OP_OPEN_PROC);
		int outerProcAddress = interp.programLength;
		interp.doTs34Line(P0019.OP_OPEN_PROC);
		int innerProcAddress = interp.programLength;
		interp.doTs34Line(P0019.OPC_PUSH_VALUE,"data:,Henry");
		interp.doTs34Line(P0019.OP_CLOSE_PROC);
		interp.doTs34Line(P0019.OP_CLOSE_PROC);
		assertDataStackEquals(new Object[] { P0019.mkProcByAddress(outerProcAddress) }, interp);

		interp.doTs34Line(P0019.OP_EXECUTE);
		assertDataStackEquals(new Object[] { P0019.mkProcByAddress(innerProcAddress) }, interp);

		interp.doTs34Line(P0019.OP_EXECUTE);
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
		new P0019Test().run();
	}
}
