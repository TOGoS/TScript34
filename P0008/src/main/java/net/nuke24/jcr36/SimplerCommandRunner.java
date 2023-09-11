package net.nuke24.jcr36;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimplerCommandRunner {
	public static String VERSION = "JCR36.1.15-dev"; // Bump to 36.1.x for 'simpler' (one-class) version
	
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
	
	protected static String quoteArr(String[] arr) {
		StringBuilder sb = new StringBuilder("[");
		String sep = "";
		for( String item : arr ) {
			sb.append(item).append(sep);
			sep = ", ";
		}
		sb.append("]");
		return sb.toString();
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
	
	public static void doJcrExit(String[] args, int i) {
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
			throw new RuntimeException("Too many arguments to jcr:exit: "+quoteArr(slice(args,i,String.class)));
		}
		System.exit(code);
	}
	
	static final Pattern OFS_PAT = Pattern.compile("^--ofs=(.*)$");
	
	public static void doJcrPrint(String[] args, int i, PrintStream out) {
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
		String _sep = "";
		for( ; i<args.length; ++i ) {
			out.print(_sep);
			out.print(args[i]);
			_sep = ofs;
		}
		out.print(suffix);
	}
	
	public static void doSysProc(String[] args, int i, Map<String,String> env) {
		String[] resolvedArgs = new String[args.length-i];
		resolvedArgs[0] = resolveProgram(args[i++], env);
		for( int j=1; i<args.length; ++i, ++j ) resolvedArgs[j] = args[i];
		ProcessBuilder pb = new ProcessBuilder(resolvedArgs);
		pb.environment().clear();
		pb.environment().putAll(env);
		Process proc;
		try {
			proc = pb.start();
			int exitCode = proc.waitFor();
			System.exit(exitCode);
		} catch (IOException e) {
			throw new RuntimeException("Failed to run process "+quoteArr(resolvedArgs), e);
		} catch (InterruptedException e) {
			throw new RuntimeException("Failed to run process "+quoteArr(resolvedArgs), e);
		}
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
	
	public static void doJcrDoCmd(String[] args, int i, Map<String,String> parentEnv, PrintStream out) {
		Map<String,String> env = parentEnv;
		
		for( ; i<args.length; ++i ) {
			int eqidx = args[i].indexOf('=');
			if( eqidx >= 1 ) {
				if( env == parentEnv ) env = new HashMap<String,String>(parentEnv);
				env.put(args[i].substring(0,eqidx), args[i].substring(eqidx+1));
			} else if( "--".equals(args[i]) ) {
				doJcrDoCmd(args, i+1, env, out);
				return;
			} else if( "--version".equals(args[i]) ) {
				doJcrPrint(new String[] { VERSION }, 0, out);
				return;
			} else if( "--help".equals(args[i]) ) {
				doJcrPrint(new String[] { VERSION, "\n", "\n", HELP_TEXT }, 0, out);
				return;
			} else if( args[i].startsWith("-") ) {
				System.err.println("Unrecognized option: "+quote(args[i]));
			} else if( "jcr:exit".equals(args[i]) ) {
				doJcrExit(args, i+1);
				return;
			} else if( "jcr:print".equals(args[i]) ) {
				doJcrPrint(args, i+1, out);
				return;
			} else if( "jcr:run".equals(args[i]) ) {
				// Basically a no-op!
			} else {
				doSysProc(args, i, env);
			}
		}
	}
	
	public static void main(String[] args) {
		doJcrDoCmd(args, 0, System.getenv(), System.out);
	}
}
