package net.nuke24.tscript34.p0019.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
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

public class JavaProjectBuilder {
	static <T> T todo(String message) {
		throw new RuntimeException("TODO: "+message);
	}
	static void debug(String message) {
		System.err.println("#DEBUG "+message);
	}
	
	interface BuildContext {
		public File tempFile() throws IOException;
		/** Create directory and any parents if they do not exist */
		public void mkdir(File dir) throws IOException;
		public int runCmd(String[] args) throws IOException;
		public OutputStreamable getStreamable(File file) throws IOException;
		public OutputStreamable getStreamable(String name) throws IOException;
		public void putFile(File f, OutputStreamable blob) throws IOException;
	}
	
	static class HostBuildContext implements BuildContext {
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
		
		@Override public int runCmd(String[] args) throws IOException {
			StringBuilder sb = new StringBuilder("$");
			for( String arg : args ) {
				sb.append(" "+arg);
			}
			debug("Running "+sb.toString()+"...");
			ProcessBuilder pb = new ProcessBuilder(args);
			Process proc = pb.start();
			try {
				return proc.waitFor();
			} catch (InterruptedException e) {
				proc.destroy();
				return -1;
			}
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
	
	static void javac(String[] sourceNames, String destPath, BuildContext ctx) throws IOException {
		File destDir = new File(destPath);
		ctx.mkdir(destDir);
		
		String[] javacCmd = new String[] { "javac", "-source", "1.6", "-target", "1.6" };
		String[] javacOpts = new String[] { "-d", destPath };
		String[] argv = concat(javacCmd, javacOpts, sourceNames);
		int exitCode = ctx.runCmd(argv);
		if( exitCode != 0 ) {
			throw new IOException("javac exited with status: "+exitCode);
		}
	}
	
	// TODO: Take a list of source root - javac options objects, to configure java version, etc
	static <T,E> T compileJar(List<File> sourceRoots, List<File> resourceRoots, Map<String,OutputStreamable> otherContent, BuildContext ctx, Procedure<OutputStreamable,? super BuildContext,? extends IOException,T> dest) throws IOException {
		final Map<String,OutputStreamable> jarContents = new LinkedHashMap<String,OutputStreamable>();
		
		OutputStreamable sourcesList = new OutputStreamable() {
			@Override
			public void writeTo(OutputStream os) throws IOException {
				Writer ps = new OutputStreamWriter(os);
				for( File sr : sourceRoots ) {
					walk(sr, sr.getPath(), new ThrowingBiConsumer<File,String,IOException>() {
						@Override
						public void accept(File a, String b) throws IOException {
							ps.write(b+"\n");
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
			javac(new String[] { "@"+sourcesListFile.getPath() }, destPath.getPath(), ctx);
			
			walk(destPath, "", add2Jar);
			todo("Collect compiled .class files to put into jarContents");
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
	
	static final Pattern SOURCES_ROOT_ARG_PATTERN = Pattern.compile("--java-sources=(.*)");
	static final Pattern MAIN_CLASS_ARG_PATTERN = Pattern.compile("--main-class=(.*)");
	// Include additional content in the JAR; --include:<filename>=<URI>
	static final Pattern INCLUDE_ITEM_PATTERN = Pattern.compile("--item:([^=]+)=(.*)");
	static final Pattern RESOURCES_ROOT_ARG_PATTERN = Pattern.compile("--resources=(.*)");
	
	public static int main(String[] args, int argi, PrintStream stdout, PrintStream errout, BuildContext ctx) {
		try {
			ArrayList<File> sourceRoots = new ArrayList<File>();
			ArrayList<File> resourceRoots = new ArrayList<File>();
			Map<String,OutputStreamable> otherContent = new HashMap<String,OutputStreamable>();
			boolean includeSources = false;
			String outPath = null;
			Matcher m;
			
			while( argi < args.length ) {
				String arg = args[argi++];
				if( "--self-test".equals(arg) ) {
					return test();
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
					todo("generate manifest, add to additional content or whatever");
				} else if( (m = RESOURCES_ROOT_ARG_PATTERN.matcher(arg)).matches() ) {
					File root = new File(m.group(1));
					resourceRoots.add(root);
				} else if( (m = SOURCES_ROOT_ARG_PATTERN.matcher(arg)).matches() ) {
					File root = new File(m.group(1));
					sourceRoots.add(root);
					if( includeSources ) {
						resourceRoots.add(root);
					}
				} else {
					errout.println("Unrecognized argument: "+arg);
				}
			}
			
			if( outPath == null ) {
				errout.println("Error: Output path not specified; `-o -` to output to stdout");
				return 1;
			}
			final String _outPath = outPath;
			
			compileJar(sourceRoots, resourceRoots, otherContent, ctx, new Procedure<OutputStreamable, BuildContext, IOException, Void>() {
				public Void apply(OutputStreamable jar, BuildContext context) throws IOException {
					writeTo(jar, _outPath, stdout, ctx);
					return null;
				}
			});
			
			return 0;
		} catch( Exception e ) {
			e.printStackTrace(errout);
			return 1;
		}
	}
	
	public static void main(String[] args) {
		System.exit(main(args, 0, System.out, System.err, new HostBuildContext()));
	}
}
