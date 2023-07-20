package net.nuke24.tscript34.p0006;

import junit.framework.TestCase;

public class P0006Test extends TestCase {
	protected <T> void assertSubArrayEquals(T[] expected, int offE, int lenE, T[] actual, int offA, int lenA) {
		assertEquals( lenE, lenA );
		for( int i=0; i<lenE; ++i ) {
			assertEquals(expected[offE+i], actual[offA+i]);
		}
	}
	
	public void testConcat() {
		P0006 interp = new P0006();
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
		P0006 interp = new P0006();
		interp.doToken("{");
		interp.doToken("1025");
		interp.doToken("}");
		assertSubArrayEquals(
			new Object[] { P0006.OP_PUSH_LITERAL_1, Integer.valueOf(1025), P0006.OP_RETURN }, 0, 3,
			interp.program, 0, interp.programLength
		);
		
		interp.pushR(-1);
		interp.run();
		assertSubArrayEquals(
			new Object[] { Integer.valueOf(1025) }, 0, 1,
			interp.dataStack, 0, interp.dsp
		);
	}
}
