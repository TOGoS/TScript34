package net.nuke24.jcr36;

import java.lang.reflect.Array;

public class ArrayUtils {
	private ArrayUtils() { }
	
	public static <T> T[] slice(T[] arr, int offset, int length, Class<T> elementClass) {
		assert( length - offset <= arr.length );
		if( offset == 0 ) return arr;
		
		@SuppressWarnings("unchecked")
		T[] newArr = (T[]) Array.newInstance(elementClass, length);
		
		for( int i=0; i<length; ++i ) {
			newArr[i] = arr[offset+i];
		}
		return newArr;
	}
	public static <T> T[] slice(T[] arr, int offset, Class<T> elementClass) {
		return slice(arr, offset, arr.length-offset, elementClass);
	}
}
