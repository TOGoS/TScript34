package net.nuke24.tscript34.p0019.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.nuke24.tscript34.p0019.iface.Consumer;
import net.nuke24.tscript34.p0019.iface.InputStreamSource;
import net.nuke24.tscript34.p0019.iface.OutputStreamable;
import net.nuke24.tscript34.p0019.iface.Procedure;

public class JavaProjectBuilder {
	interface BuildContext {
		File tempFile();
		int run(String[] args) throws IOException;
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

	class InputStreamSourceBlob implements OutputStreamable {
		final InputStreamSource source;
		public InputStreamSourceBlob(InputStreamSource source) {
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
			File[] children = f.listFiles();
			if( children == null ) return;
			String prefix = name == "" ? "" : name + "/";
			for( File c : children ) {
				walk(c, prefix+c.getName(), callback);
			}
		} else {
			callback.accept(f, name);
		}
	}
	
	static int runProc(String[] argv) throws IOException {
		ProcessBuilder pb = new ProcessBuilder(argv);
		Process proc = pb.start();
		try {
			return proc.waitFor();
		} catch (InterruptedException e) {
			proc.destroy();
			return -1;
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
	
	static void javac(String[] sourceNames, String destPath) throws IOException {
		File destDir = new File(destPath);
		if( !destDir.exists() ) {
			destDir.mkdirs();
		}
		
		String[] javacCmd = new String[] { "javac", "-source", "1.6", "-target", "1.6" };
		String[] javacOpts = new String[] { "-d", destPath };
		String[] argv = concat(javacCmd, javacOpts, sourceNames);
		int exitCode = runProc(argv);
		if( exitCode != 0 ) {
			throw new IOException("javac exited with status: "+exitCode);
		}
	}
	
	static <T> T todo(String message) {
		throw new RuntimeException("TODO: "+message);
	}
	
	static <T,E> T compileJar(List<File> sourceRoots, List<File> resourceRoots, Map<String,OutputStreamable> otherContent, boolean includeSources, BuildContext ctx, Procedure<OutputStreamable,? super BuildContext,? extends IOException,T> dest) throws IOException {
		Map<String,OutputStreamable> jarContents = new LinkedHashMap<String,OutputStreamable>();
		
		File sourcesListFile = ctx.tempFile();
		final Writer sourcesListWriter = new FileWriter(sourcesListFile, Charsets.UTF8);
		try {
			for( File sr : sourceRoots ) {
				walk(sr, "", new ThrowingBiConsumer<File,String,IOException>() {
					@Override
					public void accept(File a, String b) throws IOException {
						sourcesListWriter.write(b+"\n");
					}
				});
			}
		} finally {
			sourcesListWriter.close();
		}
		
		File destPath = ctx.tempFile();
		
		javac(new String[] { "@"+sourcesListFile.getPath() }, destPath.getPath());
		
		todo("Find generated .class files, add to jarContents");
		
		OutputStreamable jar = new ZipBlob(jarContents);
		return dest.apply(jar, ctx);
	}
	
	public static void main(String[] args) {
		todo("Implement main lmao");
	}
}
