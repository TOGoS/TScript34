package net.nuke24.jcr36;

import java.util.LinkedHashMap;

import junit.framework.TestCase;

public class SimpleCommandParserTest extends TestCase
{
	static LinkedHashMap<String,String> FOOBAR = new LinkedHashMap<String,String>();
	static {
		FOOBAR.put("foo", "bar");
	}
	
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
		JCRAction action = SimpleCommandParser.parseDoCmd(new String[] { "echo", "foo", "bar" } );
		
		assertSame( ShellCommand.class, action.getClass() );
		
		ShellCommand cmd = (ShellCommand)action;
		assertArrayEquals( new String[] { "echo", "foo", "bar" }, cmd.argv );
		
		assertEquals2(new ShellCommand(new String[]{"echo", "foo", "bar"}, ShellCommand.DEFAULT_ON_EXIT), action);
	}
	
	public void testParseHelpCommand() {
		JCRAction action = SimpleCommandParser.parseDoCmd(new String[] { "--help", "foo", "bar" } );
		
		assertEquals2(new PrintAction(SimpleCommandParser.HELP_TEXT, Streams.STDOUT_FD), action);
	}
	
	public void testParseLetCommand() {
		JCRAction action = SimpleCommandParser.parseDoCmd(new String[] { "foo=bar", "foo", "bar" } );
				
		assertEquals2(
			new LetEnv(
				FOOBAR,
				new ShellCommand(new String[] {"foo", "bar"}, ShellCommand.DEFAULT_ON_EXIT)
			),
			action
		);
	}
	
	public void testParseJcrDoCmdCommand() {
		JCRAction action = SimpleCommandParser.parseDoCmd(new String[] { "jcr:docmd", "some-program", "foo" } );
		
		assertEquals(
			new ShellCommand(new String[] { "some-program", "foo" }, ShellCommand.DEFAULT_ON_EXIT),
			action
		);
	}
	
	public void testParseJcrDoCmdCommandWithEnvVars() {
		JCRAction action = SimpleCommandParser.parseDoCmd(new String[] { "jcr:docmd", "foo=bar", "some-program", "foo" } );
		
		assertEquals(
			new LetEnv(
				FOOBAR,
				new ShellCommand(new String[] { "some-program", "foo" }, ShellCommand.DEFAULT_ON_EXIT)
			),
			action
		);
	}
	
	public void testParseExcessiveJcrDoCmdCommandWithEnvVars() {
		JCRAction action = SimpleCommandParser.parseDoCmd(new String[] { "jcr:docmd", "jcr:docmd", "foo=bar", "jcr:docmd", "jcr:docmd", "some-program", "foo" } );
		
		assertEquals(
			new LetEnv(
				FOOBAR,
				new ShellCommand(new String[] { "some-program", "foo" }, ShellCommand.DEFAULT_ON_EXIT)
			),
			action
		);
	}
	
	public void testParseJcrPrint() {
		assertEquals(
			new PrintAction("foo bar\n", Streams.STDOUT_FD),
			SimpleCommandParser.parseDoCmd(new String[] { "jcr:print", "foo", "bar" } )
		);
	}
	
	public void testParseJcrPrintN() {
		assertEquals(
			new PrintAction("foo bar", Streams.STDOUT_FD),
			SimpleCommandParser.parseDoCmd(new String[] { "jcr:print", "-n", "foo", "bar" } )
		);
	}
	
	public void testParseJcrPrintMinusMinus() {
		assertEquals(
			new PrintAction("-n foo bar\n", Streams.STDOUT_FD),
			SimpleCommandParser.parseDoCmd(new String[] { "jcr:print", "--", "-n", "foo", "bar" } )
		);
	}
	
	public void testParseJcrPrintWithGunkAroundIt() {
		JCRAction action = SimpleCommandParser.parseDoCmd(new String[] { "jcr:docmd", "jcr:docmd", "foo=bar", "jcr:print", "jcr:docmd", "some-program", "foo" } );
		
		assertEquals(
			new LetEnv(
				FOOBAR,
				new PrintAction("jcr:docmd some-program foo\n", Streams.STDOUT_FD)
			),
			action
		);
	}
}
