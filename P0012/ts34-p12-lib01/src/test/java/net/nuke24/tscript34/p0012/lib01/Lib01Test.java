package net.nuke24.tscript34.p0012.lib01;

import junit.framework.TestCase;

public class Lib01Test extends TestCase {
	public void testMe() {
		assertFalse("j00 win it!", false);
	}
	
	public void testGreeting() {
		String greet = new GreetingGenerator().greetingFor("George");
		assertEquals("Hello, George!", greet);
	}
}
