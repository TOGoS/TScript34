package net.nuke24.tscript34.p0011;

import java.lang.reflect.Array;

public class ArrayUtil {
	public static <T> T[] join(T[] a, T[] b) {
		if( b.length == 0 ) return a;
		if( a.length == 0 ) return b;
		@SuppressWarnings("unchecked")
		T[] newArr = (T[]) Array.newInstance(a.getClass().getComponentType(), a.length + b.length);
		for( int i=0; i<a.length; ++i ) newArr[i] = a[i];
		for( int i=0; i<b.length; ++i ) newArr[i+a.length] = b[i];
		return newArr;
	}
	
	public static <T> T[] slice(T[] arr, int offset, int length) {
		assert( length - offset <= arr.length );
		if( offset == 0 && length == arr.length ) return arr;
		
		@SuppressWarnings("unchecked")
		T[] newArr = (T[]) Array.newInstance(arr.getClass().getComponentType(), length);
		
		for( int i=0; i<length; ++i ) {
			newArr[i] = arr[offset+i];
		}
		return newArr;
	}
	public static <T> T[] slice(T[] arr, int offset) {
		return slice(arr, offset, arr.length-offset);
	}
}
