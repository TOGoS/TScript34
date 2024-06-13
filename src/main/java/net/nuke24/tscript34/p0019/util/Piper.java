package net.nuke24.tscript34.p0019.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class Piper
extends Thread
{
	protected InputStream in;
	protected OutputStream out;
	protected boolean ownIn, ownOut;
	public ArrayList<Throwable> errors = new ArrayList<Throwable>();
	public Piper(InputStream in, boolean ownIn, OutputStream out, boolean ownOut) {
		this.in = in; this.out = out;
	}
	@Override public void run() {
		try {
			byte[] buf = new byte[16384];
			int z;
			while( (z = in.read(buf)) > 0 ) {
				if( out != null ) out.write(buf, 0, z);
			}
		} catch( Exception e ) {
			this.errors.add(e);
		} finally {
			if( this.ownIn ) try {
				in.close();
			} catch (IOException e) {
				this.errors.add(e);
			}
			
			if( this.ownOut ) try {
				if( out != null ) out.close();
			} catch( Exception e ) {
				this.errors.add(e);
			}
		}
	}
	public static Piper start(InputStream in, boolean ownIn, OutputStream out, boolean ownOut) {
		Piper p = new Piper(in, ownIn, out, ownOut);
		p.start();
		return p;
	}
}
