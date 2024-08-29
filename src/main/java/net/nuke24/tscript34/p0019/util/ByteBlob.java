package net.nuke24.tscript34.p0019.util;

import java.io.IOException;
import java.io.OutputStream;

import net.nuke24.tscript34.p0019.iface.OutputStreamable;

class ByteBlob implements OutputStreamable {
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