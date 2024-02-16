package net.nuke24.tscript34.p0014.util;

public class ArrayUtil {
	private ArrayUtil() { }
	
	public static <T> byte[] concat(byte[] a, int offA, byte[] b) {
		if( offA == a.length ) return b;
		if( offA == 0 && b.length == 0 ) return a;
		byte[] newbuf = new byte[a.length-offA + b.length];
		for(int i=offA; i<a.length; ++i ) newbuf[i-offA] = a[i];
		for(int i=0; i<b.length; ++i ) newbuf[i+a.length-offA] = b[i];
		return newbuf;
	}
	/*
	public static <T> T[] concat(T[] a, int offA, T[] b) {
		if( offA == a.length ) return b;
		if( offA == 0 && b.length == 0 ) return a;
		@SuppressWarnings("unchecked")
		T[] newbuf = (T[])Array.newInstance(b.getClass().getComponentType(), a.length-offA + b.length);
		for(int i=offA; i<a.length; ++i ) newbuf[i-offA] = a[i];
		for(int i=0; i<b.length; ++i ) newbuf[i+a.length-offA] = b[i];
		return newbuf;
	}
	*/
	
	// -1 : not found
	// -2 : possibly found at end, need more data
	//  * : entire thing found at this offset
	public static final int find(byte[] haystack, int off, byte[] needle, boolean isEnd) {
		search: for( int i=off; i<haystack.length; ++i ) {
			for( int j=0, k=i; j<needle.length; ++j, ++k ) {
				if( k >= haystack.length ) return isEnd ? -1 : -2;
				if( needle[j] != haystack[k] ) continue search;
			}
			return i;
		}
		// Got to end without finding ourselves in a possible needle
		return -1;
	}
}
