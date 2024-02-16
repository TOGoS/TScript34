package net.nuke24.tscript34.p0014;

import java.util.Arrays;

public class LLChunks {
	private LLChunks() { }
	
	// TODO: All the chunks should include line number!
	public interface Chunk { }
	public static record NewEntryLine(String typeString, String idString) implements Chunk {
		public static NewEntryLine of(String typeString, String idString) {
			if( typeString == null ) typeString = "";
			if( idString == null ) idString = "";
			return new NewEntryLine(typeString, idString);
		}
	}
	public static record HeaderKey(String key) implements Chunk { }
	public static record HeaderValuePiece(String data) implements Chunk {}
	public static record ContentPiece(byte[] data) implements Chunk {
		@Override public boolean equals(Object other) {
			if( other instanceof ContentPiece vc ) {
				return Arrays.equals(this.data, vc.data());
			} else return false;
		}
		@Override public int hashCode() {
			return Arrays.hashCode(data);
		}
	}
	
	public static record SyntaxError(int lineNum, String message) implements Chunk { }
}
