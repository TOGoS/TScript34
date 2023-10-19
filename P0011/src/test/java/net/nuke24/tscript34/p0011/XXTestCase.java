package net.nuke24.tscript34.p0011;

import java.io.PrintStream;

import junit.framework.TestCase;
import net.nuke24.tscript34.p0011.Danducer.DucerData;
import net.nuke24.tscript34.p0011.DanducerTestUtil.ArrayChunkerator;
import net.nuke24.tscript34.p0011.DanducerTestUtil.Chunkerator;

public abstract class XXTestCase extends TestCase {
	PrintStream debugStream;
	
	protected <T> String arrayToString(T[] arr) {
		StringBuilder sb = new StringBuilder("[");
		String sep = "";
		for( T item : arr ) {
			sb.append(sep);
			sb.append(item);
			sep = " ";
		}
		sb.append("]");
		return sb.toString();
	}
	
	protected <T> void assertArrayEquals(T[] expected, T[] actual) {
		String expectedStr = arrayToString(expected);
		String actualStr = arrayToString(actual);
		if( !expectedStr.equals(actualStr) ) {
			System.err.println("Expected: "+expectedStr);
			System.err.println("Actual  : "+actualStr);
		}
		assertEquals(expected.length, actual.length);
		for( int i=0; i<expected.length; ++i ) {
			assertEquals(expected[i], actual[i]);
		}
	}
	
	protected <IS,O> void testDucerOutput(O[] expectedOutput, Chunkerator<IS> input, DucerData<IS,O[]> initialState) {
		for( int seed=0; seed<10; ++seed ) {
			boolean inputTerminates = seed % 2 == 0;
			DucerData<IS, O[]> ducerResult = DanducerTestUtil.processRandomlyChunked(
				initialState,
				input,
				inputTerminates,
				seed,
				debugStream
			);
			// Assume for now that terminating the input means that
			// the output should also be terminated.
			assertEquals(inputTerminates, ducerResult.isDone);
			assertArrayEquals(expectedOutput, ducerResult.output);
		}
	}
	
	protected <IS,O> void testDucerOutput(O[] expectedOutput, Chunkerator<IS> input, Danducer<IS,O[]> initialState) {
		testDucerOutput(
			expectedOutput,
			input,
			initialState.process(input.next(0).getHead(), false)
		);
	}
	
	protected <I,O> void testDucerOutput(O[] expectedOutput, I[] input, Danducer<I[],O[]> initialState) {
		testDucerOutput(
			expectedOutput,
			new ArrayChunkerator<I>(input),
			initialState
		);
	}
}
