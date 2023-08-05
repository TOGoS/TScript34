package net.nuke24.tscript34.p0009;

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
	
	public void testParsePushValue() {
		P0009 interpreter = new P0009();
		interpreter.definitions.put(P0009.OPC_PUSH_VALUE, P0009.mkSpecial(P0009.ST_INTRINSIC_OP_CONSTRUCTOR, P0009.OPC_PUSH_VALUE));
		Object parsed = interpreter.parseTs34_2Op(new String[] { P0009.OPC_PUSH_VALUE, "data:,hi%20there" });
		assertEquals(parsed, P0009.mkSpecial(P0009.ST_INTRINSIC_OP, P0009.OP_PUSH_LITERAL_1, "hi there"));
	}
	
	public void testDoPushValue() {
		P0009 interp = new P0009();
		interp.definitions.put(P0009.OPC_PUSH_VALUE, P0009.mkSpecial(P0009.ST_INTRINSIC_OP_CONSTRUCTOR, P0009.OPC_PUSH_VALUE));
		interp.doTs34_2Line(P0009.OPC_PUSH_VALUE+" data:,hi%20there");
		assertSubArrayEquals(
			new Object[] { "hi there" }, 0, 1,
			interp.dataStack, 0, 1
		);
	}
	
	public void run() {
		testConcat();
		testCompileDecimalNumber();
		testParsePushValue();
		testDoPushValue();
	}
	
	public static void main(String[] args) {
		new P0009Test().run();
	}
}
