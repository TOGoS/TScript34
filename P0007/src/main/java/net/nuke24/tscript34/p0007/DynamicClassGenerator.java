package net.nuke24.tscript34.p0007;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;

public class DynamicClassGenerator {
	public static void main(String[] args) throws IOException, InterruptedException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		Random r = new Random();
		String idStr = String.valueOf(r.nextInt());
		
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
		
		ProcessBuilder javacPb = new ProcessBuilder(new String[] { "javac", "-d", "target/classes", dynClassSourceFile.getPath()});
		Process javacProc = javacPb.start();
		int javacExitCode = javacProc.waitFor();
		if( javacExitCode != 0 ) {
			System.err.println("Javac exited with code: "+javacExitCode);
			System.exit(1);
		}
		
		String actualStringValue = Class.forName(dynClassFullName).getConstructor(new Class<?>[0]).newInstance().toString();
		if( !actualStringValue.equals(idStr) ) {
			System.err.println("Expected \""+idStr+"\" but got \""+actualStringValue+"\" from "+dynClassFullName+"#toString()");
			System.exit(1);
		}
	}
}
