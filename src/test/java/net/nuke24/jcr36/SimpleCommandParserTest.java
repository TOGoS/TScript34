package net.nuke24.jcr36;

import junit.framework.TestCase;

public class SimpleCommandParserTest extends TestCase
{
	protected void assertArrayEquals(Object[] a, Object[] b) {
		if( a == b ) return;
		if( a == null || b == null ) fail(a + " != " + b);
		if( a.length != b.length ) fail("length("+a+") = "+a.length + " != length("+b+") = "+b.length);
		for( int i=0; i<a.length; ++i ) {
			assertEquals(a[i], b[i]);
		}
	}
	
	public void testParseBareCommand() {
		JCRAction action = SimpleCommandParser.parse(new String[] { "echo", "foo", "bar" } );
		
		assertSame( ShellCommand.class, action.getClass() );
		
		ShellCommand cmd = (ShellCommand)action;
		assertArrayEquals( new String[] { "echo", "foo", "bar" }, cmd.argv );
	}
}
