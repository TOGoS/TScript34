package net.nuke24.tscript34.p0019.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.nuke24.tscript34.p0019.iface.OutputStreamable;

/** A blob representing a zip file to be generated from given content */
class ZipBlob implements OutputStreamable {
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