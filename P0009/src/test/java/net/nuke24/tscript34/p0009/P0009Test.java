package net.nuke24.tscript34.p0009;

import java.util.Arrays;

public class P0009Test {
	protected static <T> void assertEqualsArr(T[] a, T[] b) {
		if( a.length != b.length ) throw new RuntimeException("assertEqualsArr fails: Length not same: "+a.length+" != "+b.length);
		for( int i=0; i<a.length; ++i ) assertEquals(a[i], b[i]);
	}
	protected static void assertEquals(Object a, Object b) {
		if( a == null && b == null ) return;
		if( a == null || b == null ) {
			throw new RuntimeException("assertEquals fails: "+a+" != "+b);
		}
				
		if( a instanceof String && P0009.isSpecial(b) ) {
			b = P0009.toString(b);
		}
		
		if( (a instanceof Object[]) && (b instanceof Object[]) ) {
			assertEqualsArr((Object[])a, (Object[])b);
		} else {
			if( !a.equals(b) ) {
				throw new RuntimeException("assertEquals fails: "+a+" != "+b);
			}
		}
	}
	
	protected <T> void assertSubArrayEquals(T[] expected, int offE, int lenE, T[] actual, int offA, int lenA) {
		assertEquals( lenE, lenA );
		for( int i=0; i<lenE; ++i ) {
			assertEquals(expected[offE+i], actual[offA+i]);
		}
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
		assertSubArrayEquals(
			new Object[] { P0009.OP_PUSH_LITERAL_1, Integer.valueOf(1025), P0009.OP_RETURN }, 0, 3,
			interp.program, 0, interp.programLength
		);
		
		interp.pushR(-1);
		interp.run();
		assertSubArrayEquals(
			new Object[] { Integer.valueOf(1025) }, 0, 1,
			interp.dataStack, 0, interp.dsp
		);
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
		interp.doTs34Line(P0009.OPC_PUSH_VALUE+" data:,abc");
		interp.doTs34Line(P0009.OPC_PUSH_VALUE+" data:,def");
		interp.doTs34Line(P0009.OPC_PUSH_VALUE+" data:,ghi");
		interp.doTs34Line(P0009.OP_EXCH);
		assertSubArrayEquals(
			new Object[] { "abc", "ghi", "def" }, 0, 3,
			interp.dataStack, 0, interp.dsp
		);
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
	}
	
	public static void main(String[] args) {
		new P0009Test().run();
	}
}
