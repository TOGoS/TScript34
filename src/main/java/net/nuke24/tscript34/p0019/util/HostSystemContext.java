package net.nuke24.tscript34.p0019.util;

import static net.nuke24.tscript34.p0019.util.DebugUtil.debug;
import static net.nuke24.tscript34.p0019.util.DebugUtil.todo;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.nuke24.tscript34.p0019.iface.OutputStreamable;
import net.nuke24.tscript34.p0019.iface.SystemContext;

public class HostSystemContext implements SystemContext, Closeable {
	final File pwd;
	final Map<String,String> env;
	final List<File> tempFiles;
	
	// As a bit of a hack, the root context
	// tracks all the temp files of all its children.
	// Really, the temp files should be deleted as soon
	// as each child goes out of scope.
	
	private HostSystemContext(File pwd, Map<String,String> env, List<File> tempFiles) {
		this.pwd = pwd;
		this.env = Collections.unmodifiableMap(env);
		this.tempFiles = tempFiles;
	}
	
	static final HostSystemContext instance = new HostSystemContext(new File("."), Collections.<String,String>emptyMap(), new ArrayList<File>());
	
	public static HostSystemContext fromEnv() {
		return new HostSystemContext(new File("."), System.getenv(), new ArrayList<File>());
	}
	
	@Override public Map<String, String> getEnv() { return env; }
	@Override public File getPwd() { return pwd; }
	
	public SystemContext withEnv(Map<String,String> env) {
		return new HostSystemContext(this.pwd, env, tempFiles);
	}
	public SystemContext withPwd(File dir) {
		return new HostSystemContext(dir, this.env, tempFiles);
	}
	
	@SuppressWarnings("deprecation")
	static String fum(Date d) {
		return d.getYear()+"-"+(d.getMonth()+1)+"-"+d.getDate();
	}
	
	final Random r = new Random();
	final String prefix = fum(new Date());
	
	@Override public File tempFile(String ext) throws IOException {
		File tempFile = new File(".temp/"+prefix+"/"+r.nextLong(0,Long.MAX_VALUE)+r.nextLong(0,Long.MAX_VALUE)+ext);
		tempFiles.add(tempFile);
		return tempFile;
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
	
	protected static void rmRf(File f) {
		if( f.isDirectory() ) {
			File[] children = f.listFiles();
			if( children != null ) for( File child : children ) rmRf(child);
		}
		if( !f.delete() ) {
			System.err.println("Warning: Failed to delete temporary file "+f);
		}
	}
	
	@Override public void close() {
		for( File temp : tempFiles ) {
			rmRf(temp);
		}
	}
}
