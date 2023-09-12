package net.nuke24.jcr36;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimplerCommandRunner {
	public static final String VERSION = "JCR36.1.19-dev"; // Bump to 36.1.x for 'simpler' version
	
	public static final int EXIT_CODE_PIPING_ERROR = -1001;
	
	public static final String CMD_DOCMD = "http://ns.nuke24.net/JavaCommandRunner36/Action/DoCmd";
	public static final String CMD_EXIT = "http://ns.nuke24.net/JavaCommandRunner36/Action/Exit";
	public static final String CMD_PRINT = "http://ns.nuke24.net/JavaCommandRunner36/Action/Print";
	public static final String CMD_RUNSYSPROC = "http://ns.nuke24.net/JavaCommandRunner36/Action/RunSysProc";
	
	// Quote in the conventional C/Java/JSON style.
	// Don't rely on this for passing to other programs!
	public static String quote(String s) {
		return
			"\"" +
			s.replace("\\","\\\\")
			 .replace("\"", "\\\"")
			 .replace("\r","\\r")
			 .replace("\n","\\n")
			 .replace("\t","\\t")
			 .replace(""+(char)0x1b,"\\x1B")
			+"\"";
	}
	
	public static String debug(Object obj) {
		if( obj == null ) {
			return "null";
		} else if( obj instanceof String ) {
			return quote((String)obj);
		} else if( obj instanceof String[] ) {
			StringBuilder sb = new StringBuilder("[");
			String sep = "";
			for( Object item : (String[])obj ) {
				sb.append(sep).append(debug(item));
				sep = ", ";
			}
			sb.append("]");
			return sb.toString();
		} else {
			return "("+obj.getClass().getName()+")"+obj.toString();
		}
	}
	
	public static <T> T[] slice(T[] arr, int offset, int length, Class<T> elementClass) {
		assert( length - offset <= arr.length );
		if( offset == 0 ) return arr;
		
		@SuppressWarnings("unchecked")
		T[] newArr = (T[]) Array.newInstance(elementClass, length);
		
		for( int i=0; i<length; ++i ) {
			newArr[i] = arr[offset+i];
		}
		return newArr;
	}
	public static <T> T[] slice(T[] arr, int offset, Class<T> elementClass) {
		return slice(arr, offset, arr.length-offset, elementClass);
	}
	
	protected static String resolveProgram(String name, Map<String,String> env) {
		String pathSepRegex = Pattern.quote(File.pathSeparator);
		
		String pathsStr = env.get("PATH");
		if( pathsStr == null ) pathsStr = "";
		String[] pathParts = pathsStr.length() == 0 ? new String[0] : pathsStr.split(pathSepRegex);
		String pathExtStr = env.get("PATHEXT");
		String[] pathExts = pathExtStr == null || pathExtStr.length() == 0 ? new String[] {""} : pathExtStr.split(pathSepRegex);
		
		for( String path : pathParts ) {
			for( String pathExt : pathExts ) {
				File candidate = new File(path + File.separator + name + pathExt);
				// System.err.println("Checking for "+candidate.getPath()+"...");
				if( candidate.exists() ) return candidate.getPath();
			}
		}
		return name;
	}
	
	public static int doJcrExit(String[] args, int i) {
		int code;
		if( args.length == i ) {
			code = 0;
		} else if( args.length == i+1 ) {
			try {
				code = Integer.parseInt(args[i]);
			} catch( NumberFormatException e ) {
				throw new RuntimeException("jcr:exit: Failed to parse '"+args[i]+"' as integer", e);
			}
		} else {
			throw new RuntimeException("Too many arguments to jcr:exit: "+debug(slice(args,i,String.class)));
		}
		return code;
	}
	
	static final Pattern OFS_PAT = Pattern.compile("^--ofs=(.*)$");
	
	public static int doJcrPrint(String[] args, int i, PrintStream out) {
		String ofs = " "; // Output field separator, i.e. OFS in AWK
		String suffix = "\n";
		Matcher m;
		for( ; i<args.length; ++i ) {
			if( "-n".equals(args[i]) ) {
				suffix = "";
			} else if( (m = OFS_PAT.matcher(args[i])).matches() ) {
				ofs = m.group(1);
			} else if( "--".equals(args[i]) ) {
				++i;
				break;
			} else if( args[i].startsWith("-") ) {
				throw new RuntimeException("Unrecognized argument to jcr:print: "+quote(args[i]));
			} else {
				break;
			}
		}
		if( out == null ) return 0;
		String _sep = "";
		for( ; i<args.length; ++i ) {
			out.print(_sep);
			out.print(args[i]);
			_sep = ofs;
		}
		out.print(suffix);
		return 0;
	}
	
	static class Piper extends Thread {
		protected InputStream in;
		protected OutputStream out;
		protected boolean ownIn, ownOut;
		public ArrayList<Throwable> errors = new ArrayList<Throwable>();
		public Piper(InputStream in, boolean ownIn, OutputStream out, boolean ownOut) {
			this.in = in; this.out = out;
		}
		@Override public void run() {
			try {
				byte[] buf = new byte[16384];
				int z;
				while( (z = in.read(buf)) > 0 ) {
					if( out != null ) out.write(buf, 0, z);
				}
			} catch( Exception e ) {
				this.errors.add(e);
			} finally {
				if( this.ownIn ) try {
					in.close();
				} catch (IOException e) {
					this.errors.add(e);
				}
				
				if( this.ownOut ) try {
					if( out != null ) out.close();
				} catch( Exception e ) {
					this.errors.add(e);
				}
			}
		}
		public static Piper start(InputStream in, boolean ownIn, OutputStream out, boolean ownOut) {
			Piper p = new Piper(in, ownIn, out, ownOut);
			p.start();
			return p;
		}
	}
	
	static InputStream getInputStream(Object is) {
		if( is == null ) {
			return new ByteArrayInputStream(new byte[0]);
		} else if( is instanceof InputStream ) {
			return (InputStream)is;
		} else {
			throw new RuntimeException("Don't know how to turn "+debug(is)+" into InputStream");
		}
	}
	
	static OutputStream getOutputStream(Object os) {
		if( os == null ) return null;
		if( os instanceof OutputStream ) return (OutputStream)os;
		throw new RuntimeException("Don't know how to make OutputStream from "+os);
	}
	
	static PrintStream getPrintStream(Object os) {
		if( os == null ) return null;
		if( os instanceof PrintStream ) return (PrintStream)os;
		if( os instanceof OutputStream ) {
			try {
				return new PrintStream((OutputStream)os, false, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
		throw new RuntimeException("Don't know how to make PrintStream from "+os);
	}
	
	public static int doSysProc(String[] args, int i, Map<String,String> env, Object[] io) {
		String[] resolvedArgs = new String[args.length-i];
		resolvedArgs[0] = resolveProgram(args[i++], env);
		for( int j=1; i<args.length; ++i, ++j ) resolvedArgs[j] = args[i];
		ProcessBuilder pb = new ProcessBuilder(resolvedArgs);
		pb.environment().clear();
		pb.environment().putAll(env);
		pb.redirectInput(io[0] == System.in ? Redirect.INHERIT : Redirect.PIPE);
		pb.redirectOutput(io[1] == System.out ? Redirect.INHERIT : Redirect.PIPE);
		pb.redirectError( io[2] == System.err ? Redirect.INHERIT : Redirect.PIPE);
		Process proc;
		try {
			proc = pb.start();
			ArrayList<Piper> pipers = new ArrayList<Piper>();
			if( pb.redirectInput() == Redirect.PIPE ) pipers.add(Piper.start(getInputStream(io[0]), false, proc.getOutputStream(), true));
			if( pb.redirectOutput() == Redirect.PIPE ) pipers.add(Piper.start(proc.getInputStream(), true, getOutputStream(io[1]), false));
			if( pb.redirectError() == Redirect.PIPE ) pipers.add(Piper.start(proc.getErrorStream(), true, getOutputStream(io[2]), false));
			int exitCode = proc.waitFor();
			
			for( Piper p : pipers ) {
				p.join();
				if( !p.errors.isEmpty() && exitCode == 0 ) exitCode = EXIT_CODE_PIPING_ERROR; 
			}
			PrintStream stdErr = getPrintStream(io[2]);
			if( stdErr != null ) for( Piper p : pipers ) for( Throwable e : p.errors ) {
				stdErr.print("Piping error: "+e+"\n");
			}
			
			return exitCode;
		} catch (IOException e) {
			throw new RuntimeException("Failed to run process "+debug(resolvedArgs), e);
		} catch (InterruptedException e) {
			throw new RuntimeException("Failed to run process "+debug(resolvedArgs), e);
		}
	}
	
	public static String envMangleAlias(String name) {
		return "JCR_ALIAS_"+name.replace(":", "_").toUpperCase();
	}
	
	public static String dealiasCommand(String name, Map<String,String> env) {
		String resolved = env.get(envMangleAlias(name));
		return resolved == null ? name : resolved;
	}
	
	public static Map<String,String> STANDARD_ALIASES = new HashMap<String,String>();
	static {
		STANDARD_ALIASES.put("jcr:docmd" , CMD_DOCMD);
		STANDARD_ALIASES.put("jcr:exit"  , CMD_EXIT);
		STANDARD_ALIASES.put("jcr:print" , CMD_PRINT);
		STANDARD_ALIASES.put("jcr:runsys", CMD_RUNSYSPROC);
	}
	
	protected static String HELP_TEXT =
		"Usage: jcr36 [jcr:run] [<k>=<v> ...] [--] <command> [<arg> ...]\n"+
		"\n"+
		"Commands:\n"+
		"  # Set environment variables and run the specified sub-command:\n"+
		"  jcr:run [<k>=<v> ...] <command> [<arg> ...]\n"+
		"  \n"+
		"  # print words, separated by <separator> (defauls: one space);\n"+
		"  # -n to omit otherwise-implicit trailing newline:\n"+
		"  jcr:print [-n] [--ofs=<separator>] [--] [<word> ...]\n"+
		"  \n"+
		"  # Exit with status code:\n"+
		"  jrc:exit [<code>]";
	
	public static int doJcrDoCmd(String[] args, int i, Map<String,String> parentEnv, Object[] io) {
		Map<String,String> env = parentEnv;
		
		for( ; i<args.length; ++i ) {
			int eqidx = args[i].indexOf('=');
			if( eqidx >= 1 ) {
				if( env == parentEnv ) env = new HashMap<String,String>(parentEnv);
				env.put(args[i].substring(0,eqidx), args[i].substring(eqidx+1));
			} else if( "--".equals(args[i]) ) {
				return doJcrDoCmd(args, i+1, env, io);
			} else if( "--version".equals(args[i]) ) {
				return doJcrPrint(new String[] { VERSION }, 0, getPrintStream(io[1]));
			} else if( "--help".equals(args[i]) ) {
				return doJcrPrint(new String[] { VERSION, "\n", "\n", HELP_TEXT }, 0, getPrintStream(io[1]));
			} else if( args[i].startsWith("-") ) {
				System.err.println("Unrecognized option: "+quote(args[i]));
			} else {
				String cmd = dealiasCommand(args[i], env);
				if( CMD_EXIT.equals(cmd) ) {
					return doJcrExit(args, i+1);
				} else if( CMD_PRINT.equals(cmd) ) {
					return doJcrPrint(args, i+1, getPrintStream(io[1]));
				} else if( "jcr:run".equals(cmd) ) {
					// Basically a no-op!
				} else if( CMD_RUNSYSPROC.equals(cmd) ) {
					return doSysProc(args, i+1, env, io);
				} else {
					return doSysProc(args, i, env, io);
				}
			}
		}
		return 0;
	}
	
	public static Map<String,String> withAliases(Map<String,String> env, Map<String,String> aliases) {
		if( aliases.size() == 0 ) return env;
		env = new HashMap<String,String>(env);
		for( Map.Entry<String,String> ae : aliases.entrySet() ) {
			env.put(envMangleAlias(ae.getKey()), ae.getValue());
		}
		return env;
	}
	
	public static void main(String[] args) {
		int argi = 0;
		boolean loadStdAliases = true;
		if( "--no-std-aliases".equals(args[argi]) ) {
			loadStdAliases = false;
			++argi;
		}
		Map<String,String> env = System.getenv();
		env = withAliases(env, loadStdAliases ? STANDARD_ALIASES : Collections.<String,String>emptyMap());
		int exitCode = doJcrDoCmd(args, argi, env, new Object[] { System.in, System.out, System.err });
		System.exit(exitCode);
	}
}
