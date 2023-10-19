package net.nuke24.tscript34.p0011;

import java.util.Arrays;

import junit.framework.TestCase;

public class ArrayUtilTest extends TestCase {
	final String[] arrOfFoo = new String[] { "foo" };
	final String[] arrOfBar = new String[] { "bar" };
	final String[] arrOfBaz = new String[] { "baz" };
	final String[] arrOfFooBar = new String[] { "foo", "bar" };
	final String[] arrOfFooBarBaz = new String[] { "foo", "bar", "baz" };
	final String[] emptyArray = new String[0];
	
	public void testJoin() {
		assertEquals(
			Arrays.asList(arrOfFooBarBaz),
			Arrays.asList(ArrayUtil.join(arrOfFooBar, arrOfBaz))
		);
	}
	public void testJoinToNothing() {
		assertSame(
			arrOfFooBar,
			ArrayUtil.join(arrOfFooBar, emptyArray)
		);
		assertSame(
			arrOfFooBar,
			ArrayUtil.join(emptyArray, arrOfFooBar)
		);
	}
	
	public void testSlice() {
		assertEquals(
			Arrays.asList(arrOfBar),
			Arrays.asList(ArrayUtil.slice(arrOfFooBarBaz, 1, 1))
		);
	}
	public void testSliceFirst() {
		assertEquals(
			Arrays.asList(arrOfFoo),
			Arrays.asList(ArrayUtil.slice(arrOfFooBar, 0, 1))
		);
	}
	public void testSliceToEnd() {
		assertEquals(
			Arrays.asList(arrOfBaz),
			Arrays.asList(ArrayUtil.slice(arrOfFooBarBaz, 2))
		);
	}
}
