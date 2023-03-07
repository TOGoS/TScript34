package net.nuke24.tscript34;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

public class P0003TestCaseTest extends TestCase {
	static class P0003TestCase {
		public P0003TestCase() { }
		public File psSourceFile;
		public File ts34SourceFile;
		public File outputFile;
	}
	
	protected static P0003TestCase getTestCase(Map<String,P0003TestCase> testCases, String name) {
		P0003TestCase testCase = testCases.get(name);
		if( testCase == null ) {
			testCases.put(name, testCase = new P0003TestCase());
		}
		return testCase;
	}
	
	public static Map<String,P0003TestCase> loadTestCases(File testCaseDir) {
		Map<String,P0003TestCase> testCases = new HashMap<>();
		
		Pattern p3tcFilenamePattern = Pattern.compile("^(.*)\\.(ts34|ps|output)$");
		
		for( File f : testCaseDir.listFiles() ) {
			Matcher m = p3tcFilenamePattern.matcher(f.getName());
			if( !m.matches() ) continue;
			String tcName = m.group(1);
			String tcExt = m.group(2);
			P0003TestCase testCase = getTestCase(testCases, tcName);
			if( "ts34".equals(tcExt) ) {
				testCase.ts34SourceFile = f;
			} else if( "ps".equals(tcExt) ) {
				testCase.psSourceFile = f;
			} else if( "output".equals(tcExt) ) {
				testCase.outputFile = f;
			} else {
				throw new RuntimeException("Oops, unhandled test case filename extension: '"+tcExt+"'");
			}
		}
		
		return testCases;
	}
	
	protected static String slurp(Reader r) throws IOException {
		char[] buffer = new char[1024];
		StringWriter sw = new StringWriter();
		int z;
		while( (z = r.read(buffer)) > 0 ) {
			sw.write(buffer, 0, z);
		}
		return sw.toString();
	}
	protected static String slurp(File f) throws IOException {
		try( Reader r = new FileReader(f) ) {
			return slurp(r);
		}
	}
	
	public void testPsToTs34(File psSourceFile, File ts34SourceFile) throws IOException {
		FileReader psReader = new FileReader(psSourceFile);
		StringWriter ts34Writer = new StringWriter();
		new PSToTS34Translator(ts34Writer).translate(psReader);
		
		String generatedTs34Source = ts34Writer.toString();
		String expectedTs34Source = slurp(ts34SourceFile);
		
		System.err.println("Comparing:\n"+generatedTs34Source+"\nto:\n"+expectedTs34Source);
		
		assertEquals(generatedTs34Source, expectedTs34Source);
	}
	
	public void test(P0003TestCase testCase) throws IOException {
		if( testCase.psSourceFile != null && testCase.ts34SourceFile != null ) {
			testPsToTs34(testCase.psSourceFile, testCase.ts34SourceFile);
		}
	}
	
	public void testRunTheTestCases() throws IOException {
		Map<String,P0003TestCase> testCases = loadTestCases(new File("../P0003/test-cases"));
		System.err.println("Found "+testCases.size()+" test cases");
		assertTrue( "Expected to find some test cases", testCases.size() > 0 );
		for( P0003TestCase testCase : testCases.values() ) {
			test(testCase);
		}
	}
}
