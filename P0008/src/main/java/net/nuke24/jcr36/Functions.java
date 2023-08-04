package net.nuke24.jcr36;

public class Functions {
	public static final Function<?,?> IDENTITY = new Function<Object,Object>() {
		public Object apply(Object in) {
			return in;
		}
	};
	
	@SuppressWarnings("unchecked")
	public static <I> Function<I,I> identity() {
		return (Function<I, I>) IDENTITY;
	}
}
