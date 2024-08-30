package net.nuke24.tscript34.p0019.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipUtil
{
	static class Dirmkator {
		File lastMkd;
		public void mkdirs(File dir) throws IOException {
			if( lastMkd == null || !lastMkd.equals(dir) ) {
				if( dir.isDirectory() ) {
					// Nothing to do!
				} else if( dir.exists() ) {
					throw new IOException(dir+" already exists but is NOT a directory!");
				} else {
					if( !dir.mkdirs() ) {
						throw new IOException("Failed to make "+dir);
					}
				}
				lastMkd = dir;
			}
		}
	}
	
	public static void unzipTo(File zipFile, File dest) throws IOException {
		ZipFile zip = new ZipFile(zipFile);
		Dirmkator dirmkator = new Dirmkator();
		try {
			Enumeration<? extends ZipEntry> zipEntryEnumerator = zip.entries();
			while( zipEntryEnumerator.hasMoreElements() ) {
				ZipEntry entry = zipEntryEnumerator.nextElement();
				File entryFile = new File(dest, entry.getName());
				if( entry.isDirectory() ) {
					dirmkator.mkdirs(entryFile);
				} else {
					dirmkator.mkdirs(entryFile.getParentFile());
					FileOutputStream os = new FileOutputStream(entryFile);
					try {
						Piper.pipe(zip.getInputStream(entry), true, os, false);
					} finally {
						os.close();
					}
				}
			}
		} finally {
			zip.close();
		}
	}
}
