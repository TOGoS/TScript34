package net.nuke24.tscript34.p0019.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.nuke24.tscript34.p0019.iface.InputStreamSource;

class FileInputStreamSource implements InputStreamSource {
	protected File file;
	FileInputStreamSource(File file) {
		this.file = file;
	}
	public InputStream getInputStream() throws IOException {
		return new FileInputStream(this.file);
	}
}