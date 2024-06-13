package net.nuke24.tscript34.p0019.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.ProcessBuilder.Redirect;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.nuke24.tscript34.p0019.P0019;

// Adapted from JCR36's procedure of the same name
public class SysProcRunner
{
	static InputStream toInputStream(Object is) {
		if( is == null ) {
			return new ByteArrayInputStream(new byte[0]);
		} else if( is instanceof InputStream ) {
			return (InputStream)is;
		} else {
			throw new RuntimeException("Don't know how to turn "+DebugFormat.toDebugString(is)+" into InputStream");
		}
	}
	
	static OutputStream totOutputStream(Object os) {
		if( os == null ) return null;
		if( os instanceof OutputStream ) return (OutputStream)os;
		throw new RuntimeException("Don't know how to make OutputStream from "+os);
	}
	
	protected static <T> T[] cons(T item, T[] list) {
		@SuppressWarnings("unchecked")
		T[] newList = (T[])Array.newInstance(list.getClass().getComponentType(), list.length + 1);
		newList[0] = item;
		for( int i=0; i<list.length; ++i ) newList[i+1] = list[i];
		return newList;
	}
	
	protected static List<String> resolvePrograms(String name, Map<String,String> env, PrintStream debugStream) {
		String pathSepRegex = Pattern.quote(File.pathSeparator);
		
		String pathsStr = env.get("PATH");
		if( pathsStr == null ) pathsStr = env.get("Path"); // For Windows compatibility
		if( pathsStr == null ) pathsStr = "";
		String[] pathParts = pathsStr.length() == 0 ? new String[0] : pathsStr.split(pathSepRegex);
		String pathExtStr = env.get("PATHEXT");
		String[] pathExts = pathExtStr == null || pathExtStr.length() == 0 ? new String[] {} : pathExtStr.split(pathSepRegex);
		pathExts = cons("", pathExts);
		if( debugStream != null ) {
			debugStream.println("PATH: "+pathsStr);
			debugStream.println("Path separator: "+File.pathSeparator);
			debugStream.println("Path separator regex: "+pathSepRegex);
			debugStream.print("PATH items: ");
			String sep = "";
			for( String path : pathParts ) {
				debugStream.print(sep+path);
				sep = ", ";
			}
			debugStream.println();
			debugStream.println("PATHEXT: "+pathExtStr);
			debugStream.print("PATHEXT items: ");
			sep = "";
			for( String ext : pathExts ) {
				debugStream.print(sep+ext);
				sep = ", ";
			}
			debugStream.println();
		}
		
		List<String> results = new ArrayList<String>();
		
		for( String path : pathParts ) {
			for( String pathExt : pathExts ) {
				File candidate = new File(path + File.separator + name + pathExt);
				if( debugStream != null ) debugStream.println("Checking for "+candidate.getPath()+"...");
				if( candidate.exists() ) {
					if( debugStream != null ) debugStream.println("Found "+candidate.getPath());
					results.add(candidate.getPath());
				}
			}
		}
		
		return results;
	}
	
	protected static String resolveProgram(String name, Map<String,String> env) {
		List<String> x = resolvePrograms(name, env, null);
		return x.size() == 0 ? name : x.get(0);
	}
	
	public static int doSysProc(String[] args, int i, File pwd, Map<String,String> env, Object[] io) {
		String[] resolvedArgs = new String[args.length-i];
		resolvedArgs[0] = resolveProgram(args[i++], env);
		for( int j=1; i<args.length; ++i, ++j ) resolvedArgs[j] = args[i];
		ProcessBuilder pb = new ProcessBuilder(resolvedArgs);
		pb.environment().clear();
		pb.environment().putAll(env);
		pb.directory(pwd);
		pb.redirectInput(io[0] == System.in ? Redirect.INHERIT : Redirect.PIPE);
		pb.redirectOutput(io[1] == System.out ? Redirect.INHERIT : Redirect.PIPE);
		pb.redirectError( io[2] == System.err ? Redirect.INHERIT : Redirect.PIPE);
		Process proc;
		try {
			proc = pb.start();
			ArrayList<Piper> pipers = new ArrayList<Piper>();
			if( pb.redirectInput() == Redirect.PIPE ) pipers.add(Piper.start(toInputStream(io[0]), false, proc.getOutputStream(), true));
			if( pb.redirectOutput() == Redirect.PIPE ) pipers.add(Piper.start(proc.getInputStream(), true, totOutputStream(io[1]), false));
			if( pb.redirectError() == Redirect.PIPE ) pipers.add(Piper.start(proc.getErrorStream(), true, totOutputStream(io[2]), false));
			int exitCode = proc.waitFor();
			
			for( Piper p : pipers ) {
				p.join();
				if( !p.errors.isEmpty() && exitCode == 0 ) exitCode = P0019.EXIT_CODE_PIPING_ERROR; 
			}
			PrintStream stdErr = StreamUtil.toPrintStream(io[2]);
			if( stdErr != null ) for( Piper p : pipers ) for( Throwable e : p.errors ) {
				stdErr.print("Piping error: "+e+"\n");
			}
			
			return exitCode;
		} catch (IOException e) {
			throw new RuntimeException("Failed to run process "+DebugFormat.toDebugString(resolvedArgs)+" (pwd="+pwd+")", e);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted while running process "+DebugFormat.toDebugString(resolvedArgs)+" (pwd="+pwd+")", e);
		}
	}
}
