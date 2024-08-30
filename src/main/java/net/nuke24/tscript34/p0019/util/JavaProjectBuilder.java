package net.nuke24.tscript34.p0019.util;

import static net.nuke24.tscript34.p0019.util.DebugUtil.debug;
import static net.nuke24.tscript34.p0019.util.DebugUtil.todo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.nuke24.tscript34.p0019.iface.OutputStreamable;
import net.nuke24.tscript34.p0019.iface.Procedure;
import net.nuke24.tscript34.p0019.iface.SystemContext;

public class JavaProjectBuilder {
	
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
	
	/**
	 * TIP: Make sure sourceNames are absolute paths!
	 * Otherwise they will be interpreted relative to
	 * whatever the ctx has as the pwd, which might
	 * not be the same as the system pwd when this is called!
	 */
	static void javac(String[] sourceNames, File destDir, OutputStream errout, SystemContext ctx) throws IOException {
		ctx.mkdir(destDir);
		
		String sourceVer = ctx.getEnv().get("JAVAC_SOURCE_VERSION");
		if( sourceVer == null || sourceVer.isEmpty() ) sourceVer = "1.6";
		String targetVer = ctx.getEnv().get("JAVAC_TARGET_VERSION");
		if( targetVer == null || targetVer.isEmpty() ) targetVer = "1.6";
		
		String[] javacCmd = new String[] { "javac", "-source", sourceVer, "-target", targetVer };
		String[] javacOpts = new String[] { "-d", destDir.getAbsolutePath() };
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
		final SystemContext ctx,
		final Procedure<OutputStreamable,? super SystemContext,? extends IOException,T> dest
	) throws IOException {
		final Map<String,OutputStreamable> jarContents = new LinkedHashMap<String,OutputStreamable>();
		
		OutputStreamable sourcesList = new OutputStreamable() {
			@Override
			public void writeTo(OutputStream os) throws IOException {
				final PrintStream ps = StreamUtil.toPrintStream(os);
				for( File sr : sourceRoots ) {
					walk(sr, sr.getAbsolutePath(), new ThrowingBiConsumer<File,String,IOException>() {
						// The BiConsumer thing might be overly fancy, here.
						// Could have just called getAbsolutePath on every File.
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
		
		File sourcesListFile = ctx.tempFile(".lst");
		ctx.putFile(sourcesListFile, sourcesList);
		if( sourcesListFile.length() == 0 ) {
			debug("No sources found, skipping javac");
		} else {
			File destDir = ctx.tempFile("-classes");
			ctx.mkdir(destDir);
			
			javac(new String[] { "@"+sourcesListFile.getAbsolutePath() }, destDir, errout, ctx);
			
			walk(destDir, "", add2Jar);
		}
		
		for( File resourceRoot : resourceRoots ) {
			walk(resourceRoot, "", add2Jar);
		}
		
		jarContents.putAll(otherContent);
		
		OutputStreamable jar = new ZipBlob(jarContents);
		return dest.apply(jar, ctx);
	}
	
	static void writeTo(OutputStreamable blob, String destName, OutputStream stdout, SystemContext ctx) throws IOException {
		if( "-".equals(destName) ) {
			blob.writeTo(stdout);
		} else {
			ctx.putFile(new File(destName), blob);
		}
	}
	
	public static int test() {
		return todo("Implement self-test lmao, or maybe delete it and do it with a script.");
	}
	
	static final Pattern SOURCES_ROOT_ARG_PATTERN = Pattern.compile("--java-sources=(.*)");
	static final Pattern MAIN_CLASS_ARG_PATTERN = Pattern.compile("--main-class=(.*)");
	// Include additional content in the JAR; --include:<filename>=<URI>
	static final Pattern INCLUDE_ITEM_PATTERN = Pattern.compile("--item:([^=]+)=(.*)");
	static final Pattern RESOURCES_ROOT_ARG_PATTERN = Pattern.compile("--resources=(.*)");
	
	public static int compileJarMain(
		final String[] args, int argi,
		final InputStream stdin,
		final PrintStream stdout,
		final PrintStream errout,
		SystemContext ctx
	) throws Exception {
		ArrayList<File> sourceRoots = new ArrayList<File>();
		ArrayList<File> resourceRoots = new ArrayList<File>();
		Map<String,OutputStreamable> otherContent = new HashMap<String,OutputStreamable>();
		boolean includeSources = false;
		String outPath = null;
		Matcher m;
		
		while( argi < args.length ) {
			String arg = args[argi++];
			if( "-o".equals(arg) ) {
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
				String destName = m.group(1);
				String sourceName = m.group(2);
				otherContent.put(outPath, ctx.getStreamable(sourceName));
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
		final SystemContext _ctx = ctx;
		
		compileJar(
			sourceRoots, resourceRoots, otherContent,
			errout, ctx,
			new Procedure<OutputStreamable, SystemContext, IOException, Void>() {
				public Void apply(OutputStreamable jar, SystemContext context) throws IOException {
					writeTo(jar, _outPath, stdout, _ctx);
					return null;
				}
			}
		);
		
		return 0;
	}
}
