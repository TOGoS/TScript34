package net.nuke24.tscript34.p0014;

import java.util.Arrays;
import java.util.Objects;

public class LLChunks {
	private LLChunks() { }
	
	protected static String quote(String s) {
		if( s == null ) return "null";
		return "\"" + s.replace("\\","\\\\").replace("\"","\\\"")+"\"";
	}
	protected static String quote(byte[] s) {
		if( s == null ) return "null";
		return s.length+" bytes";
	}
	
	// TODO: All the chunks should include line number!
	public interface Chunk { }
	public static class NewEntryLine implements Chunk {
		public final String typeString;
		public final String idString;
		public NewEntryLine(String typeString, String idString) {
			this.typeString = typeString;
			this.idString = idString;
		}
		public static NewEntryLine of(String typeString, String idString) {
			if( typeString == null ) typeString = "";
			if( idString == null ) idString = "";
			return new NewEntryLine(typeString, idString);
		}
		@Override public String toString() {
			return "NewEntryLine[typeString="+quote(typeString)+", idString="+quote(idString)+"]";
		}
		@Override public boolean equals(Object other) {
			if( !(other instanceof NewEntryLine) ) return false;
			
			NewEntryLine onel = (NewEntryLine)other;
			return
				Objects.equals(this.typeString, onel.typeString) &&
				Objects.equals(this.idString, onel.idString);
		}
		@Override public int hashCode() {
			return 100 + (Objects.hashCode(this.typeString)<<4) + (Objects.hashCode(this.idString)<<8);
		}
	}
	
	public static class Header implements Chunk {
		public final String key;
		public final String value;
		public Header(String key, String value) {
			this.key = key;
			this.value = value;
		}
		@Override public String toString() {
			return "Header[key="+quote(key)+", value="+quote(value)+"]";
		}
		@Override public boolean equals(Object other) {
			if( !(other instanceof Header) ) return false;
			
			Header oh = (Header)other;
			return Objects.equals(this.key, oh.key) && Objects.equals(this.value, oh.value);
		}
		@Override public int hashCode() {
			return 200 + (Objects.hashCode(this.key)<<4) + (Objects.hashCode(this.value)<<8);
		}
	}
	public static class HeaderKey implements Chunk {
		public final String key;
		public HeaderKey(String key) {
			this.key = key;
		}
		@Override public String toString() {
			return "HeaderKey[key="+quote(key)+"]";
		}
		@Override public boolean equals(Object other) {
			if( !(other instanceof HeaderKey) ) return false;
			
			HeaderKey ohk = (HeaderKey)other;
			return Objects.equals(this.key, ohk.key);
		}
		@Override public int hashCode() {
			return 300 + (Objects.hashCode(this.key)<<4);
		}
	}
	public static class HeaderValuePiece implements Chunk {
		public final String data;
		public HeaderValuePiece(String data) {
			this.data = data;
		}
		@Override public String toString() {
			return "HeaderValuePiece[data="+quote(data)+"]";
		}
		@Override public boolean equals(Object other) {
			if( !(other instanceof HeaderValuePiece) ) return false;
			
			HeaderValuePiece ohvp = (HeaderValuePiece)other;
			return Objects.equals(this.data, ohvp.data);
		}
		@Override public int hashCode() {
			return 400 + (Objects.hashCode(this.data)<<4);
		}
	}
	public static class ContentPiece implements Chunk {
		public final byte[] data;
		public ContentPiece(byte[] data) {
			this.data = data;
		}
		@Override public String toString() {
			return "ContentPiece[data=" + quote(data)+"]";
		}
		@Override public boolean equals(Object other) {
			if( other instanceof ContentPiece ) {
				ContentPiece vc = (ContentPiece)other;
				return Arrays.equals(this.data, vc.data);
			} else return false;
		}
		@Override public int hashCode() {
			return Arrays.hashCode(data);
		}
	}
	
	public static class SyntaxError implements Chunk {
		public final int lineNum;
		public final String message;
		public SyntaxError(int lineNum, String message) {
			this.lineNum = lineNum;
			this.message = message;
		}
		@Override public String toString() {
			return "SyntaxError[lineNum="+lineNum+", message="+quote(message)+"]";
		}
	}
}
