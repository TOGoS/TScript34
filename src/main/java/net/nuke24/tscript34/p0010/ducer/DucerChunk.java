package net.nuke24.tscript34.p0010.ducer;

public class DucerChunk<T> {
	public final T payload;
	public final boolean isEnd;
	public DucerChunk(T data, boolean isEnd) {
		this.payload = data;
		this.isEnd = isEnd;
	}
	public boolean isEnd() {
		return isEnd;
	}
	@Override public boolean equals(Object o) {
		if( !(o instanceof DucerChunk) ) return false;
		
		// TODO: May want to properly compare byte arrays, etc
		DucerChunk<?> c = (DucerChunk<?>)o;
		return
			(this.payload == c.payload || (payload != null && c.payload != null && payload.equals(c.payload))) &&
			this.isEnd == c.isEnd;
	}
	@Override public int hashCode() {
		// TODO: May want to properly hashCode byte arrays, etc
		return (payload == null ? 0 : payload.hashCode()) ^ (isEnd ? 1 : 0);
	}
}