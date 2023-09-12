package net.nuke24.jcr36;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Collections;

import static net.nuke24.jcr36.SimplerCommandRunner.quote;
import static net.nuke24.jcr36.SimplerCommandRunner.debug;

public class SimplerCommandRunnerTest implements Runnable
{
	static class OutputCollector extends PrintStream {
		ByteArrayOutputStream baos;
		Charset charset;
		public OutputCollector(ByteArrayOutputStream baos, boolean autoFlush, Charset charset) throws UnsupportedEncodingException {
			super(baos, false, charset);
			this.baos = baos;
			this.charset = charset;
		}
		public String toString() {
			return new String(baos.toByteArray(), this.charset);
		}
		
		public static OutputCollector create() {
			try {
				return new OutputCollector(new ByteArrayOutputStream(), false, Charset.forName("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	static void assertEquals(Object a, Object b) {
		if( a == b ) return;
	
		if( a == null ) throw new RuntimeException("null != "+debug(b));
		if( b == null ) throw new RuntimeException(debug(a)+" != null");
		if( !a.equals(b) ) {
			throw new RuntimeException(debug(a)+" != "+debug(b));
		}
	}
	
	static void assertTrue(boolean condition, String message) {
		if( condition ) return;
		
		throw new RuntimeException("assertTrue(false, "+quote(message)+")");
	}
	
	public void testPrint() {
		OutputCollector out = OutputCollector.create();
		int exitCode = SimplerCommandRunner.doJcrDoCmd(new String[]{ "jcr:print", "Hello, world!" }, 0, Collections.<String,String>emptyMap(), new Object[] { null, out, null });
		assertEquals(0, exitCode);
		assertEquals("Hello, world!\n", out.toString());
	}
	
	public void testPrintN() {
		OutputCollector out = OutputCollector.create();
		int exitCode = SimplerCommandRunner.doJcrDoCmd(new String[]{ "jcr:print", "-n", "Hello, world!" }, 0, Collections.<String,String>emptyMap(), new Object[] { null, out, null });
		assertEquals(0, exitCode);
		assertEquals("Hello, world!", out.toString());
	}
	
	public void testExit() {
		assertEquals(0, SimplerCommandRunner.doJcrDoCmd(new String[]{ "jcr:exit" }, 0, Collections.<String,String>emptyMap(), new Object[] { null, null, null }));
	}
	
	public void testExit123() {
		assertEquals(123, SimplerCommandRunner.doJcrDoCmd(new String[]{ "jcr:exit", "123" }, 0, Collections.<String,String>emptyMap(), new Object[] { null, null, System.err }));
	}
	
	public void testExitN456() {
		assertEquals(-456, SimplerCommandRunner.doJcrDoCmd(new String[]{ "jcr:exit", "-456" }, 0, Collections.<String,String>emptyMap(), new Object[] { null, null, System.err }));
	}
	
	public void testRunSysProc() {
		OutputCollector out = OutputCollector.create();
		int exitCode = SimplerCommandRunner.doJcrDoCmd(new String[]{ "jcr:runsys", "java", "-version" }, 0, Collections.<String,String>emptyMap(), new Object[] { null, out, out });
		assertEquals(0, exitCode);
		assertTrue(out.toString().length() > 0, "Expected `jcr:runsys java -version` to output some non-zero number of characters");
	}
	
	public void testRunJcrAsSysProc() {
		File jarFile = new File("JCR36.1.14.jar");
		if( !jarFile.exists() ) {
			System.err.println("testRunJcrAsSysProc: Skipping because "+jarFile.getPath()+" doesn't exist");
			return;
		}
		OutputCollector out = OutputCollector.create();
		int exitCode = SimplerCommandRunner.doJcrDoCmd(
			new String[]{ "jcr:runsys", "java", "-jar", jarFile.getPath(), "jcr:print", "-n", "Hello, world!" },
			0, Collections.<String,String>emptyMap(), new Object[] { null, out, System.err });
		assertEquals(0, exitCode);
		assertEquals("Hello, world!", out.toString());
	}
	
	@Override public void run() {
		testPrint();
		testPrintN();
		testExit();
		testExit123();
		testExitN456();
		testRunSysProc();
		testRunJcrAsSysProc();
	}
	
	public static void main(String[] args) {
		new SimplerCommandRunnerTest().run();
		System.out.println("All tests passed!");
	}
}
