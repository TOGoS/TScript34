package net.nuke24.tscript34.p0019.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.nuke24.tscript34.p0019.iface.InputStreamSource;
import net.nuke24.tscript34.p0019.iface.OutputStreamable;
import net.nuke24.tscript34.p0019.iface.Procedure;

import static net.nuke24.tscript34.p0019.util.DebugUtil.todo;
import static net.nuke24.tscript34.p0019.util.DebugUtil.debug;

public class JavaProjectBuilder {
	public static final String VERSION = "0.2.0";
	public static final String PROGRAM_NAME = "TScript34-P0019-JavaProjectBuilder-v"+VERSION;
	
	interface BuildContext {
		public File getPwd();
		public Map<String,String> getEnv();
		public BuildContext withEnv(Map<String,String> env);
		public BuildContext withPwd(File dir);
		public File tempFile() throws IOException;
		/** Create directory and any parents if they do not exist */
		public void mkdir(File dir) throws IOException;
		public int runCmd(String[] args, Object[] io) throws IOException;
		public OutputStreamable getStreamable(File file) throws IOException;
		public OutputStreamable getStreamable(String name) throws IOException;
		public void putFile(File f, OutputStreamable blob) throws IOException;
	}
	
	static class HostBuildContext implements BuildContext {
		final File pwd;
		final Map<String,String> env;
		
		private HostBuildContext(File pwd, Map<String,String> env) {
			this.pwd = pwd;
			this.env = Collections.unmodifiableMap(env);
		}
		
		static final HostBuildContext instance = new HostBuildContext(new File("."), Collections.<String,String>emptyMap());
		
		static HostBuildContext fromEnv() {
			return new HostBuildContext(new File("."), System.getenv());
		}
		
		@Override public Map<String, String> getEnv() { return env; }
		@Override public File getPwd() { return pwd; }
		
		public BuildContext withEnv(Map<String,String> env) {
			return new HostBuildContext(this.pwd, env);
		}
		public BuildContext withPwd(File dir) {
			return new HostBuildContext(dir, this.env);
		}
		
		@SuppressWarnings("deprecation")
		static String fum(Date d) {
			return d.getYear()+"-"+(d.getMonth()+1)+"-"+d.getDate();
		}
		
		final Random r = new Random();
		final String prefix = fum(new Date());
		
		@Override public File tempFile() throws IOException {
			return new File(".temp/"+prefix+"/"+r.nextLong()+r.nextLong());
		}
		
		@Override public void mkdir(File dir) throws IOException {
			if( !dir.exists() ) {
				if( dir.mkdirs() == false ) {
					throw new IOException("Failed to create directory '"+dir+"'");
				}
			}
		}
		
		@Override public int runCmd(String[] args, Object[] io) throws IOException {
			return SysProcRunner.doSysProc(args, 0, pwd, env, io);
		}
		
		@Override public OutputStreamable getStreamable(File file) throws IOException {
			return new InputStreamOutputStreamable(new FileInputStreamSource(file));
		}
		
		@Override public OutputStreamable getStreamable(String name) throws IOException {
			return todo("getStreamable(\""+name+"\"");
		}
		
		/**
		 * Put the given content at the specified file.
		 * Create parent directories as necessary.
		 * If a file exists, delete it (write a *new* file; don't rewrite the old one)
		 */
		@Override public void putFile(File f, OutputStreamable blob) throws IOException {
			File dir = f.getParentFile();
			if( dir != null ) mkdir(dir);
			File tempFile = new File(dir, "."+f.getName()+".temp"+new Random().nextLong());
			FileOutputStream fos = new FileOutputStream(tempFile);
			boolean success = false;
			try {
				blob.writeTo(fos);
				success = true;
			} finally {
				fos.close();
				if( success ) {
					if( f.exists() ) {
						if( !f.delete() ) {
							throw new IOException("Failed to delete "+f+"; to-be-written contents remain in "+tempFile);
						}
					}
					if( !tempFile.renameTo(f) ) {
						throw new IOException("Failed to rename "+tempFile+" to "+f);
					}
				} else {
					debug("Deleting "+f+" due to exception while writing");
					tempFile.delete();
				}
			}
		}
		
		public static File resolveRelative(File pwd, String rel) {
			return new File(PathUtil.resolveFilePath(pwd, rel, false));
		}
	}
	
	static class FileInputStreamSource implements InputStreamSource {
		protected File file;
		FileInputStreamSource(File file) {
			this.file = file;
		}
		public InputStream getInputStream() throws IOException {
			return new FileInputStream(this.file);
		}
	}
	
	static class InputStreamOutputStreamable implements OutputStreamable {
		final InputStreamSource source;
		public InputStreamOutputStreamable(InputStreamSource source) {
			this.source = source;
		}
		
		@Override public void writeTo(OutputStream os) throws IOException {
			InputStream is = this.source.getInputStream();
			try {
				byte[] buffer = new byte[65536];
				int z;
				while( (z = is.read(buffer)) > 0 ) {
					os.write(buffer, 0, z);
				}
			} finally {
				is.close();
			}
		}
	}
	
	static class ByteBlob implements OutputStreamable {
		final byte[] data;
		public ByteBlob(byte[] data) {
			this.data = data;
		}
		
		@Override
		public void writeTo(OutputStream os) throws IOException {
			os.write(this.data);
		}
		
		public static ByteBlob of(String str) {
			return new ByteBlob(str.getBytes(Charsets.UTF8));
		}
	}
	
	/** A blob representing a zip file to be generated from given content */
	static class ZipBlob implements OutputStreamable {
		final Map<String,OutputStreamable> content;
		public ZipBlob(Map<String,OutputStreamable> content) {
			this.content = content;
		}
		
		@Override
		public void writeTo(OutputStream os) throws IOException {
			ZipOutputStream zos = new ZipOutputStream(os);
			try {
				for( Map.Entry<String,OutputStreamable> e : content.entrySet() ) {
					zos.putNextEntry(new ZipEntry(e.getKey()));
					e.getValue().writeTo(zos);
				}
			} finally {
				zos.close();
			}
		}
	}
	
	static String makeJarManifest(String mainClassName) {
		return
			"Manifest-Version: 1.0\n" +
			"Main-Class: "+mainClassName+"\n";
	}
	
	static ZipBlob makeZip(Map<String,OutputStreamable> contents) {
		return new ZipBlob(contents);
	}
	
	static ZipBlob makeJar(Map<String,OutputStreamable> contents) {
		LinkedHashMap<String,OutputStreamable> withManifest = new LinkedHashMap<String,OutputStreamable>();
		withManifest.putAll(contents);
		return new ZipBlob(withManifest);
	}
	
	interface ThrowingBiConsumer<A,B,E extends Throwable> {
		public void accept(A a, B b) throws E;
	}
	
	static <E extends Throwable> void walk(File f, String name, ThrowingBiConsumer<File,String,E> callback) throws E {
		if( f.isDirectory() ) {
			debug("Walking "+f+"...");
			File[] children = f.listFiles();
			if( children == null ) return;
			String prefix = name == "" ? "" : name + "/";
			for( File c : children ) {
				walk(c, prefix+c.getName(), callback);
			}
		} else {
			debug("Found regular file "+f+"...");
			callback.accept(f, name);
		}
	}
	
	static <T> int totalLength(T[][] arrays) {
		int totalLength = 0;
		for( T[] arr : arrays ) {
			totalLength += arr.length;
		}
		return totalLength;
	}
	
	static String[] concat(String[]...arrays) {
		String[] result = new String[totalLength(arrays)];
		int i=0;
		for( String[] arr : arrays ) {
			for( int j=0; j<arr.length; ++j ) {
				result[i++] = arr[j];
			}
		}
		return result;
	}
	
	static void javac(String[] sourceNames, String destPath, OutputStream errout, BuildContext ctx) throws IOException {
		File destDir = new File(destPath);
		ctx.mkdir(destDir);
		
		String sourceVer = ctx.getEnv().get("JAVAC_SOURCE_VERSION");
		if( sourceVer == null || sourceVer.isEmpty() ) sourceVer = "1.6";
		String targetVer = ctx.getEnv().get("JAVAC_TARGET_VERSION");
		if( targetVer == null || targetVer.isEmpty() ) targetVer = "1.6";
		
		String[] javacCmd = new String[] { "javac", "-source", sourceVer, "-target", targetVer };
		String[] javacOpts = new String[] { "-d", destPath };
		String[] argv = concat(javacCmd, javacOpts, sourceNames);
		int exitCode = ctx.runCmd(argv, new Object[] {null,null,errout} );
		if( exitCode != 0 ) {
			throw new IOException("javac exited with status: "+exitCode);
		}
	}
	
	static final Pattern JAVA_SOURCE_FILENAME_PATTERN = Pattern.compile("(.*)\\.java$", Pattern.CASE_INSENSITIVE);
	
	// TODO: Take a list of source root - javac options objects, to configure java version, etc
	static <T,E> T compileJar(
		final List<File> sourceRoots,
		final List<File> resourceRoots,
		final Map<String,OutputStreamable> otherContent,
		final OutputStream errout,
		final BuildContext ctx,
		final Procedure<OutputStreamable,? super BuildContext,? extends IOException,T> dest
	) throws IOException {
		final Map<String,OutputStreamable> jarContents = new LinkedHashMap<String,OutputStreamable>();
		
		OutputStreamable sourcesList = new OutputStreamable() {
			@Override
			public void writeTo(OutputStream os) throws IOException {
				final PrintStream ps = StreamUtil.toPrintStream(os);
				for( File sr : sourceRoots ) {
					walk(sr, sr.getPath(), new ThrowingBiConsumer<File,String,IOException>() {
						@Override
						public void accept(File a, String b) throws IOException {
							if( JAVA_SOURCE_FILENAME_PATTERN.matcher(a.getName()).matches() ) {
								ps.println(b);
							}
						}
					});
				}
				ps.flush();
			}
		};
		
		ThrowingBiConsumer<File, String, IOException> add2Jar = new ThrowingBiConsumer<File, String, IOException>() {
			@Override public void accept(File f, String path) throws IOException {
				jarContents.put(path, ctx.getStreamable(f));
			}
		};
		
		File sourcesListFile = ctx.tempFile();
		ctx.putFile(sourcesListFile, sourcesList);
		if( sourcesListFile.length() == 0 ) {
			debug("No sources found, skipping javac");
		} else {
			File destPath = ctx.tempFile();
			ctx.mkdir(destPath);
			// TODO
			javac(new String[] { "@"+sourcesListFile.getPath() }, destPath.getPath(), errout, ctx);
			
			walk(destPath, "", add2Jar);
		}
		
		for( File resourceRoot : resourceRoots ) {
			walk(resourceRoot, "", add2Jar);
		}
		
		jarContents.putAll(otherContent);
		
		OutputStreamable jar = new ZipBlob(jarContents);
		return dest.apply(jar, ctx);
	}
	
	static void writeTo(OutputStreamable blob, String hey, OutputStream stdout, BuildContext ctx) throws IOException {
		if( "-".equals(hey) ) {
			blob.writeTo(stdout);
		} else {
			ctx.putFile(new File(hey), blob);
		}
	}
	
	public static int test() {
		return todo("Implement self-test lmao, or maybe delete it and do it with a script.");
	}
	
	static final String indentSection(String indent, String text) {
		return text.replaceAll("\n", "\n"+indent);
	}
	
	static final String PRINT_HELP_CMD_NAME = "ts34p19:print-help";
	static final String PRINT_HELP_HELP_TEXT = PRINT_HELP_CMD_NAME+" ; print help\n";
	
	static final String PRINT_VERSION_CMD_NAME = "ts34p19:print-version";
	static final String PRINT_VERSION_HELP_TEXT = PRINT_VERSION_CMD_NAME+" ; print version\n";
	
	static final String PB_SELF_TEST_CMD_NAME = "ts34p19:project-builder-test";
	static final String PB_SELF_TEST_HELP_TEXT = PB_SELF_TEST_CMD_NAME+" ; run project builder self-test\n";
	
	static final String COMPILE_JAR_CMD_NAME = "ts34p19:compile-jar";
	static final String COMPILE_JAR_HELP_TEXT =
		COMPILE_JAR_CMD_NAME+" <options>\n"+
		"\n" +
		"Options:\n" +
		"  -o <path>            ; path to write JAR file\n" +
		"  --include-sources    ; include following source files in the JAR\n" +
		"  --java-sources=<dir> ; compile .java source files from source root <dir>\n" +
		"  --resources=<dir>    ; include resource files within <dir>\n" +
		"  --main-class=<classname> ; indicate the specified class as main\n" +
		"";
	
	static final String HELP_TEXT =
		"Usage: JavaProjectBuilder <options> <sub-command>\n"  +
		"\n" +
		"General options:\n" +
		"  --cd=<dir>           ; Use <dir> as 'current directory' for remaining operations\n" +
		"                       ; (doesn't necessarily apply to interpretation of --options)\n" +
		"\n" +
		"Subcommands:\n" +
		"\n" +
		indentSection("  ", COMPILE_JAR_HELP_TEXT)+
		"\n" +
		indentSection("  ", PRINT_HELP_HELP_TEXT)+
		"\n" +
		indentSection("  ", PRINT_VERSION_HELP_TEXT)+
		"\n" +
		indentSection("  ", PB_SELF_TEST_HELP_TEXT)+
		"";
	
	static final Pattern CD_PATTERN = Pattern.compile("--cd=(.*)");
	static final Pattern SOURCES_ROOT_ARG_PATTERN = Pattern.compile("--java-sources=(.*)");
	static final Pattern MAIN_CLASS_ARG_PATTERN = Pattern.compile("--main-class=(.*)");
	// Include additional content in the JAR; --include:<filename>=<URI>
	static final Pattern INCLUDE_ITEM_PATTERN = Pattern.compile("--item:([^=]+)=(.*)");
	static final Pattern RESOURCES_ROOT_ARG_PATTERN = Pattern.compile("--resources=(.*)");
	
	static final int ARGMODE_NORMAL = 0;
	static final int ARGMODE_OPS    = 1;
	static final int ARGMODE_ARGS   = 2;
	
	public static int scriptMain(
		final String[] args,
		int argi,
		int argMode, // 0 = regular, 1 = --ops, 2 = slurp remaining into args
		final InputStream stdin,
		final PrintStream stdout,
		final PrintStream errout,
		BuildContext ctx
	) {
		// Each arg can be interpreted as an op.
		// If op is a reference to a file or non-op URI,
		// then the 'op' is the script described by that file.
		// Easy peasy!
		// Unless we want to suppor the traditional argv to scripts,
		// which would be semantically at odds when more than one argument.
		//
		// Traditional mode;
		// '--' is implied in this case
		//   <op> <arg1> <arg2> <arg3>
		// 
		// List-several-ops-or-scripts mode:
		//   -c <op1> -c <op2> -c <op3>
		// 
		// Each-arg-is-an-op mode;
		// '--ops' all
		// --ops <op1> <op2> <op3> -- <arg1> <arg2> <arg3>
		// 
		// Since each argument is an op, an argument may be
		// an op constructor+argument(s), so long as it is quoted.
		// 
		// Maybe P0019 should handle all this.
		
		List<String> ops = new ArrayList<String>();
		List<String> scriptArgs = new ArrayList<String>();
		
		while( argi < args.length ) {
			String arg = args[argi++];
			if( argMode == ARGMODE_ARGS ) {
				scriptArgs.add(arg);
			} else if( "-c".equals(arg) ) {
				if( argi >= args.length ) {
					errout.println("Error: `-c` requires another argument");
					return 1;
				}
				ops.add(args[argi++]);
			} else if( "--ops".equals(arg) ) {
				argMode = ARGMODE_OPS;
			} else if( "--".equals(arg) ) {
				argMode = ARGMODE_ARGS;
			} else if( !arg.startsWith("-") || arg.equals("-") ) {
				ops.add(arg);
				if( argMode == ARGMODE_NORMAL ) {
					argMode = ARGMODE_ARGS;
				}
			} else {
				errout.println("Error: Unrecognized script argument: '"+arg+"'");
				return 1;
			}
		}

		String argsStr = "";
		String sep = "";
		for( String arg : scriptArgs ) {
			argsStr += sep + "\""+arg.replace("\\","\\\\").replace("\"", "\\\"")+"\"";
			sep = " ";
		}

		// TODO: Use P0019
		List<Object> stack = new ArrayList<>();
		for( String op : ops ) {
			if( op.startsWith("pushstr:") ) {
				stack.add(op.substring(8));
			} else if( "args".equals(op) ) {
				stack.add(argsStr);
			} else if( "cd".equals(op) ) {
				String path = stack.remove(stack.size()-1).toString();
				File dir = HostBuildContext.resolveRelative(ctx.getPwd(), path);
				ctx = ctx.withPwd(dir);
			} else if( "pwd".equals(op) ) {
				stack.add(ctx.getPwd());
			} else if( "println".equals(op) ) {
				stdout.println(stack.remove(stack.size()-1));
			} else if( "print".equals(op) ) {
				stdout.print(stack.remove(stack.size()-1));
			} else if( "exit".equals(op) ) {
				return 0;
			} else if( "exit/code".equals(op) ) {
				return Integer.valueOf(""+stack.remove(stack.size()-1));
			} else {
				todo("Implement op '"+op+"', or just do this right");
			}
		}
		
		return 0;
	}
	
	public static int compileJarMain(
		final String[] args, int argi,
		final InputStream stdin,
		final PrintStream stdout,
		final PrintStream errout,
		BuildContext ctx
	) throws Exception {
		ArrayList<File> sourceRoots = new ArrayList<File>();
		ArrayList<File> resourceRoots = new ArrayList<File>();
		Map<String,OutputStreamable> otherContent = new HashMap<String,OutputStreamable>();
		boolean includeSources = false;
		String outPath = null;
		Matcher m;
		
		while( argi < args.length ) {
			String arg = args[argi++];
			if( "--version".equals(arg) ) {
				stdout.println(PROGRAM_NAME);
				return 0;
			} else if( "--help".equals(arg) ) {
				stdout.println(PROGRAM_NAME);
				stdout.println();
				stdout.print(HELP_TEXT);
				return 0;
			} else if( "-o".equals(arg) ) {
				if( outPath != null ) {
					errout.println("Error: output path already specified as '"+outPath+"'");
					return 1;
				}
				if( args.length <= argi ) {
					errout.println("Error: '-o' must be followed by a path or '-'");
				}
				outPath = args[argi++];
			} else if( "--include-sources".equals(arg) ) {
				includeSources = true;
			} else if( (m = INCLUDE_ITEM_PATTERN.matcher(arg)).matches() ) {
				todo("add item to be included");
			} else if( (m = MAIN_CLASS_ARG_PATTERN.matcher(arg)).matches() ) {
				otherContent.put("META-INF/MANIFEST.MF", ByteBlob.of(
					"Manifest-Version: 1.0\r\n"+
					"Main-Class: "+m.group(1)+"\r\n"
				));
			} else if( (m = RESOURCES_ROOT_ARG_PATTERN.matcher(arg)).matches() ) {
				File root = new File(ctx.getPwd(), m.group(1));
				resourceRoots.add(root);
			} else if( (m = SOURCES_ROOT_ARG_PATTERN.matcher(arg)).matches() ) {
				File root = new File(ctx.getPwd(), m.group(1));
				sourceRoots.add(root);
				if( includeSources ) {
					resourceRoots.add(root);
				}
			} else {
				errout.println("Unrecognized argument: "+arg);
				return 1;
			}
		}
		
		if( outPath == null ) {
			errout.println("Error: Output path not specified; `-o -` to output to stdout");
			return 1;
		}
		final String _outPath = outPath;
		final BuildContext _ctx = ctx;
		
		compileJar(
			sourceRoots, resourceRoots, otherContent,
			errout, ctx,
			new Procedure<OutputStreamable, BuildContext, IOException, Void>() {
				public Void apply(OutputStreamable jar, BuildContext context) throws IOException {
					writeTo(jar, _outPath, stdout, _ctx);
					return null;
				}
			}
		);
		
		return 0;
	}
	
	public static int main(
		final String[] args, int argi,
		final InputStream stdin,
		final PrintStream stdout,
		final PrintStream errout,
		BuildContext ctx
	) {
		try {
			Matcher m;
			
			while( argi < args.length ) {
				String arg = args[argi++];
				
				// Translate some option-looking arguments to commands
				if( "--help".equals(arg) || "-?".equals(arg) ) {
					arg = PRINT_HELP_CMD_NAME;
				} else if( "--version".equals(arg) ) {
					arg = PRINT_VERSION_CMD_NAME;
				}
				
				if( COMPILE_JAR_CMD_NAME.equals(arg) ) {
					return compileJarMain(args, argi, stdin, stdout, errout, ctx);
				} else if( PB_SELF_TEST_CMD_NAME.equals(arg) ) {
					return test();
				} else if( PRINT_HELP_CMD_NAME.equals(arg) ) {
					stdout.println(PROGRAM_NAME);
					stdout.println();
					stdout.print(HELP_TEXT);
					return 0;
				} else if( PRINT_VERSION_CMD_NAME.equals(arg) ) {
					stdout.println(PROGRAM_NAME);
					return 0;
				} else if( (m = CD_PATTERN.matcher(arg)).matches() ) {
					ctx = ctx.withPwd(HostBuildContext.resolveRelative(ctx.getPwd(), m.group(1)));
				} else if( !arg.startsWith("-") || arg.equals("-") || "-c".equals(arg) || "--ops".equals(arg) ) {
					return scriptMain(args, --argi, ARGMODE_NORMAL, stdin, stdout, errout, ctx);
				} else {
					errout.println("Unrecognized argument: "+arg);
					return 1;
				}
			}
			
			errout.println("Warning: No command given");
			
			return 0;
		} catch( Exception e ) {
			e.printStackTrace(errout);
			return 1;
		}
	}
	
	public static void main(String[] args) {
		int exitCode = main(args, 0, System.in, System.out, System.err, HostBuildContext.fromEnv());
		debug("Exiting with code "+exitCode);
		System.exit(exitCode);
	}
}
