package net.nuke24.tscript34.p0019.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.nuke24.tscript34.p0019.iface.InputStreamSource;
import net.nuke24.tscript34.p0019.iface.OutputStreamable;

class InputStreamOutputStreamable implements OutputStreamable {
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