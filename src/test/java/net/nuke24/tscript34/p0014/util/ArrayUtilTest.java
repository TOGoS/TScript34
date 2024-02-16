package net.nuke24.tscript34.p0014.util;

import java.util.Arrays;

import junit.framework.TestCase;
import net.nuke24.tscript34.p0014.util.ArrayUtil;

public class ArrayUtilTest extends TestCase
{
	protected void assertEquals(byte[] a, byte[] b) {
		assertTrue(Arrays.equals(a, b));
	}
	
	final byte[] EMPTY_BYTE_ARRAY = new byte[0];
	final byte[] THREE_BYTE_ARRAY = new byte[] {1,2,3};
	final byte[] FOUR_BYTE_ARRAY = new byte[] {1,2,3,4};
	
	public void testConcatEmpties() {
		assertEquals(EMPTY_BYTE_ARRAY, ArrayUtil.concat(EMPTY_BYTE_ARRAY, 0, EMPTY_BYTE_ARRAY));
	}
	public void testConcatEffectivelyEmpties() {
		byte[] catted = ArrayUtil.concat(THREE_BYTE_ARRAY, 3, EMPTY_BYTE_ARRAY);
		assertEquals(EMPTY_BYTE_ARRAY, catted);
		assertSame(EMPTY_BYTE_ARRAY, catted);
	}
	
	public void testConcatNonEmptyWithEmpty() {
		byte[] catted = ArrayUtil.concat(THREE_BYTE_ARRAY, 0, EMPTY_BYTE_ARRAY);
		assertEquals(THREE_BYTE_ARRAY, catted);
		assertSame(THREE_BYTE_ARRAY, catted);
	}
	
	public void testConcatEmptyWithNonEmpty() {
		byte[] catted = ArrayUtil.concat(FOUR_BYTE_ARRAY, 4, THREE_BYTE_ARRAY);
		assertEquals(THREE_BYTE_ARRAY, catted);
		assertSame(THREE_BYTE_ARRAY, catted);
	}
	
	public void testConcatNonEmpties() {
		byte[] catted = ArrayUtil.concat(FOUR_BYTE_ARRAY, 2, THREE_BYTE_ARRAY);
		assertEquals(new byte[] {3,4,1,2,3}, catted);
	}
	
	
	public void testFindZeroWidth() {
		assertEquals(1, ArrayUtil.find(THREE_BYTE_ARRAY, 1, EMPTY_BYTE_ARRAY, false));
	}
	public void testFindSingle() {
		assertEquals(2, ArrayUtil.find(THREE_BYTE_ARRAY, 1, new byte[] {3}, false));
	}
	public void testFindDouble() {
		for( int off=0; off<=1; ++off ) {
			assertEquals(1, ArrayUtil.find(THREE_BYTE_ARRAY, off, new byte[] {2,3}, false));
		}
	}
	public void testDontFindSkippedDouble() {
		for( int off=2; off<=3; ++off ) {
			assertEquals(-1, ArrayUtil.find(THREE_BYTE_ARRAY, off, new byte[] {2,3}, false));
		}
	}
	public void testFindTripleTricky() {
		for( int off=0; off<=3; ++off ) {
			assertEquals(3, ArrayUtil.find(new byte[] { 1, 2, 3, 2, 3, 4 }, off, new byte[] {2,3,4}, false));
		}
	}
	public void testFindPartialTripleTricky() {
		for( int off=0; off<=3; ++off ) {
			assertEquals(-2, ArrayUtil.find(new byte[] { 1, 2, 3, 2, 3 }, off, new byte[] {2,3,4}, false));
		}
	}
	public void testFindNoTripleDueToEof() {
		for( int off=0; off<=3; ++off ) {
			assertEquals(-1, ArrayUtil.find(new byte[] { 1, 2, 3, 2, 3 }, off, new byte[] {2,3,4}, true));
		}
	}
}
