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
	
	@Override public void run() {
		OutputCollector out = OutputCollector.create();
		SimplerCommandRunner.doJcrDoCmd(new String[]{ "jcr:print", "Hello, world!" }, 0, Collections.<String,String>emptyMap(), out);
		assertEquals("Hello, world!\n", out.toString());
	}
	
	public static void main(String[] args) {
		new SimplerCommandRunnerTest().run();
		System.out.println("All tests passed!");
	}
}
