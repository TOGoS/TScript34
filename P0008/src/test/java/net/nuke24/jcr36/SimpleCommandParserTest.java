package net.nuke24.jcr36;

import java.util.LinkedHashMap;

import junit.framework.TestCase;
import net.nuke24.jcr36.action.JCRAction;
import net.nuke24.jcr36.action.LetEnv;
import net.nuke24.jcr36.action.Print;
import net.nuke24.jcr36.action.RunExternalProgram;

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
		
		assertSame( RunExternalProgram.class, action.getClass() );
		
		RunExternalProgram cmd = (RunExternalProgram)action;
		assertArrayEquals( new String[] { "echo", "foo", "bar" }, cmd.argv );
		
		assertEquals2(new RunExternalProgram(new String[]{"echo", "foo", "bar"}, RunExternalProgram.DEFAULT_ON_EXIT), action);
	}
	
	public void testParseHelpCommand() {
		JCRAction action = SimpleCommandParser.parseDoCmd(new String[] { "--help", "foo", "bar" } );
		
		assertEquals2(new Print(SimpleCommandParser.HELP_TEXT, Streams.STDOUT_FD), action);
	}
	
	public void testParseLetCommand() {
		JCRAction action = SimpleCommandParser.parseDoCmd(new String[] { "foo=bar", "foo", "bar" } );
				
		assertEquals2(
			new LetEnv(
				FOOBAR,
				new RunExternalProgram(new String[] {"foo", "bar"}, RunExternalProgram.DEFAULT_ON_EXIT)
			),
			action
		);
	}
	
	public void testParseJcrDoCmdCommand() {
		JCRAction action = SimpleCommandParser.parseDoCmd(new String[] { "jcr:docmd", "some-program", "foo" } );
		
		assertEquals(
			new RunExternalProgram(new String[] { "some-program", "foo" }, RunExternalProgram.DEFAULT_ON_EXIT),
			action
		);
	}
	
	public void testParseJcrDoCmdCommandWithEnvVars() {
		JCRAction action = SimpleCommandParser.parseDoCmd(new String[] { "jcr:docmd", "foo=bar", "some-program", "foo" } );
		
		assertEquals(
			new LetEnv(
				FOOBAR,
				new RunExternalProgram(new String[] { "some-program", "foo" }, RunExternalProgram.DEFAULT_ON_EXIT)
			),
			action
		);
	}
	
	public void testParseExcessiveJcrDoCmdCommandWithEnvVars() {
		JCRAction action = SimpleCommandParser.parseDoCmd(new String[] { "jcr:docmd", "jcr:docmd", "foo=bar", "jcr:docmd", "jcr:docmd", "some-program", "foo" } );
		
		assertEquals(
			new LetEnv(
				FOOBAR,
				new RunExternalProgram(new String[] { "some-program", "foo" }, RunExternalProgram.DEFAULT_ON_EXIT)
			),
			action
		);
	}
	
	public void testParseJcrPrint() {
		assertEquals(
			new Print("foo bar\n", Streams.STDOUT_FD),
			SimpleCommandParser.parseDoCmd(new String[] { "jcr:print", "foo", "bar" } )
		);
	}
	
	public void testParseJcrPrintN() {
		assertEquals(
			new Print("foo bar", Streams.STDOUT_FD),
			SimpleCommandParser.parseDoCmd(new String[] { "jcr:print", "-n", "foo", "bar" } )
		);
	}
	
	public void testParseJcrPrintMinusMinus() {
		assertEquals(
			new Print("-n foo bar\n", Streams.STDOUT_FD),
			SimpleCommandParser.parseDoCmd(new String[] { "jcr:print", "--", "-n", "foo", "bar" } )
		);
	}
	
	public void testParseJcrPrintWithGunkAroundIt() {
		JCRAction action = SimpleCommandParser.parseDoCmd(new String[] { "jcr:docmd", "jcr:docmd", "foo=bar", "jcr:print", "jcr:docmd", "some-program", "foo" } );
		
		assertEquals(
			new LetEnv(
				FOOBAR,
				new Print("jcr:docmd some-program foo\n", Streams.STDOUT_FD)
			),
			action
		);
	}
	
	public void testParseVersion() {
		assertEquals(
			new Print(Versions.JCR_NAME_AND_VERSION+"\n", Streams.STDOUT_FD),
			SimpleCommandParser.parseDoCmd(new String[] { "--version" } )
		);
	}
}
