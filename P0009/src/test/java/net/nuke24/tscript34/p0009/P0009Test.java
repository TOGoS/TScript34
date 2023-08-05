package net.nuke24.tscript34.p0009;

public class P0009Test {
	protected static void assertEquals(Object a, Object b) {
		if( a == null && b == null ) return;
		
		if( a instanceof String && P0009.isSpecial(b) ) {
			b = P0009.toString(b);
		}
		
		if( a == null || b == null || !a.equals(b) ) {
			throw new RuntimeException("assertEquals fails: "+a+" != "+b);
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
	
	public void run() {
		testConcat();
		testCompileDecimalNumber();
	}
	
	public static void main(String[] args) {
		new P0009Test().run();
	}
}
