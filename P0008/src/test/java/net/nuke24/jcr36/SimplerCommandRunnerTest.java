package net.nuke24.jcr36;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Collections;

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
	
	static String debug(Object obj) {
		if( obj instanceof String ) {
			return SimplerCommandRunner.quote((String)obj);
		} else {
			return obj.toString();
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
	
	public void testPrint() {
		OutputCollector out = OutputCollector.create();
		SimplerCommandRunner.doJcrDoCmd(new String[]{ "jcr:print", "Hello, world!" }, 0, Collections.<String,String>emptyMap(), new Object[] { null, out, null });
		assertEquals("Hello, world!\n", out.toString());
	}
	
	public void testPrintN() {
		OutputCollector out = OutputCollector.create();
		SimplerCommandRunner.doJcrDoCmd(new String[]{ "jcr:print", "-n", "Hello, world!" }, 0, Collections.<String,String>emptyMap(), new Object[] { null, out, null });
		assertEquals("Hello, world!", out.toString());
	}
	
	public void testExit() {
		assertEquals(0, SimplerCommandRunner.doJcrDoCmd(new String[]{ "jcr:exit" }, 0, Collections.<String,String>emptyMap(), new Object[] { null, null, null }));
	}
	
	public void testExit123() {
		assertEquals(123, SimplerCommandRunner.doJcrDoCmd(new String[]{ "jcr:exit", "123" }, 0, Collections.<String,String>emptyMap(), new Object[] { null, null, null }));
	}
	
	public void testExitN456() {
		assertEquals(-456, SimplerCommandRunner.doJcrDoCmd(new String[]{ "jcr:exit", "-456" }, 0, Collections.<String,String>emptyMap(), new Object[] { null, null, null }));
	}
	
	@Override public void run() {
		testPrint();
		testPrintN();
		testExit();
		testExit123();
		testExitN456();
	}
	
	public static void main(String[] args) {
		new SimplerCommandRunnerTest().run();
		System.out.println("All tests passed!");
	}
}
