package net.nuke24.tscript34.p0007;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;

public class DynamicClassGenerator {
	static String cmdToString(String[] argv) {
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for( String arg : argv ) {
			sb.append(sep).append(arg);
			sep =" ";
		}
		return sb.toString();
	}
	
	public static void main(String[] args) throws IOException, InterruptedException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		final String selfName = DynamicClassGenerator.class.getSimpleName();
		boolean verbosity = false;
		for( int i=0; i<args.length; ++i ) {
			if( "--verbose".equals(args[i]) || "-v".equals(args[i]) ) {
				verbosity = true;
			} else {
				System.err.println(selfName+": Error: Unrecognized argument: "+args[i]);
				System.exit(1);
			}
		}
		
		Random r = new Random();
		String idStr = String.valueOf(r.nextInt() & 0x7FFFFFFF);
		
		String dynClassPackageName = "net.nuke24.tscript34.p0007.dyn";
		String dynClassSimpleName = "DynClass"+idStr;
		String dynClassFullName = dynClassPackageName+"."+dynClassSimpleName;
		String dynClassSource =
			"package "+dynClassPackageName+";\n" +
			"\n" +
			"public class "+dynClassSimpleName+" {\n" +
			"\tpublic String toString() { return \"" + idStr + "\"; }\n" + 
			"}\n";
		
		File generatedSourceDir = new File("src/generated/java");
		File dynPackageSourceDir = new File(generatedSourceDir, dynClassPackageName.replace('.', File.separatorChar));
		if( !dynPackageSourceDir.exists() ) dynPackageSourceDir.mkdirs();
		
		File dynClassSourceFile = new File(dynPackageSourceDir, dynClassSimpleName+".java");
		FileWriter fw = new FileWriter(dynClassSourceFile);
		try {
			fw.write(dynClassSource);
		} finally {
			fw.close();
		}
		
		String[] javacCmd = new String[] { "javac", "-d", "target/classes", dynClassSourceFile.getPath()};
		ProcessBuilder javacPb = new ProcessBuilder(javacCmd);
		javacPb.redirectError(ProcessBuilder.Redirect.INHERIT);
		javacPb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
		if(verbosity) {
			System.out.println("# "+selfName+"$ "+cmdToString(javacCmd));
		}
		Process javacProc = javacPb.start();
		int javacExitCode = javacProc.waitFor();
		if( javacExitCode != 0 ) {
			System.err.println(selfName+": Error: Javac exited with code: "+javacExitCode);
			System.exit(1);
		}
		
		String actualStringValue = Class.forName(dynClassFullName).getConstructor(new Class<?>[0]).newInstance().toString();
		if( !actualStringValue.equals(idStr) ) {
			System.err.println(selfName+": Error: Expected \""+idStr+"\" but got \""+actualStringValue+"\" from "+dynClassFullName+"#toString()");
			System.exit(1);
		}
		
		if( verbosity ) {
			System.out.println("# "+selfName+": Correct value returned by "+dynClassFullName+"#toString()");
		}
	}
}
