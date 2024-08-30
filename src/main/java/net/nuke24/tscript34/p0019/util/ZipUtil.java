package net.nuke24.tscript34.p0019.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.nuke24.tscript34.p0019.iface.OutputStreamable;
import net.nuke24.tscript34.p0019.iface.SystemContext;

public class ZipUtil
{
	public static void unzipTo(File zipFile, File dest, SystemContext ctx) throws IOException {
		final ZipFile zip = new ZipFile(zipFile);
		try {
			Enumeration<? extends ZipEntry> zipEntryEnumerator = zip.entries();
			while( zipEntryEnumerator.hasMoreElements() ) {
				final ZipEntry entry = zipEntryEnumerator.nextElement();
				final File destFile = new File(dest, entry.getName());
				if( entry.isDirectory() ) {
					ctx.mkdir(destFile);
				} else {
					ctx.putFile(destFile, new OutputStreamable() {
						@Override
						public void writeTo(OutputStream os) throws IOException {
							Piper.pipe(zip.getInputStream(entry), true, os, false);
						}
					});
				}
			}
		} finally {
			zip.close();
		}
	}
}
