package net.nuke24.jcr36;

import java.util.Map;

import junit.framework.TestCase;

public class SimpleCommandParserTest extends TestCase
{
	protected void assertEquals2(Object a, Object b) {
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}
	
	protected void assertArrayEquals(Object[] a, Object[] b) {
		if( a == b ) return;
		if( a == null || b == null ) fail(a + " != " + b);
		if( a.length != b.length ) fail("length("+a+") = "+a.length + " != length("+b+") = "+b.length);
		for( int i=0; i<a.length; ++i ) {
			assertEquals2(a[i], b[i]);
		}
	}
	
	public void testParseBareCommand() {
		JCRAction action = SimpleCommandParser.parse(new String[] { "echo", "foo", "bar" } );
		
		assertSame( ShellCommand.class, action.getClass() );
		
		ShellCommand cmd = (ShellCommand)action;
		assertArrayEquals( new String[] { "echo", "foo", "bar" }, cmd.argv );
		
		assertEquals2(new ShellCommand(new String[]{"echo", "foo", "bar"}, ShellCommand.DEFAULT_ON_EXIT), action);
	}
	
	public void testParseHelpCommand() {
		JCRAction action = SimpleCommandParser.parse(new String[] { "--help", "foo", "bar" } );
		
		assertEquals2(new PrintAction(SimpleCommandParser.HELP_TEXT), action);
	}
	
	public void testParseLetCommand() {
		JCRAction action = SimpleCommandParser.parse(new String[] { "foo=bar", "foo", "bar" } );
		
		assertEquals2(
			new LetEnv(
				Map.of("foo","bar"),
				new ShellCommand(new String[] {"foo", "bar"}, ShellCommand.DEFAULT_ON_EXIT)
			),
			action
		);		
	}
}
