package net.nuke24.tscript34.p0019.iface;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/** Various 'system' functions */
public interface SystemContext {
	public File getPwd();
	public Map<String,String> getEnv();
	public SystemContext withEnv(Map<String,String> env);
	public SystemContext withPwd(File dir);
	public File tempFile() throws IOException;
	/** Create directory and any parents if they do not exist */
	public void mkdir(File dir) throws IOException;
	public int runCmd(String[] args, Object[] io) throws IOException;
	public OutputStreamable getStreamable(File file) throws IOException;
	public OutputStreamable getStreamable(String name) throws IOException;
	public void putFile(File f, OutputStreamable blob) throws IOException;
}
