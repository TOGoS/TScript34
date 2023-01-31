package net.nuke24.tscript34;

interface HasSourceLocation {
	String getSourceFilename();
	int getSourceLineNumber();
	int getSourceColumnNumber();
	int getSourceEndLineNumber();
	int getSourceEndColumnNumber();
}

class SourceLocations {
	public static boolean sourceLocationsAreEqual(HasSourceLocation a, HasSourceLocation b) {
		return
			a.getSourceFilename().equals(b.getSourceFilename()) &&
			a.getSourceLineNumber() == b.getSourceLineNumber()	 &&
			a.getSourceColumnNumber() == b.getSourceColumnNumber() &&
			a.getSourceEndLineNumber() == b.getSourceEndLineNumber() &&
			a.getSourceEndColumnNumber() == b.getSourceEndColumnNumber();
	}
}

public class Token implements HasSourceLocation {
	enum QuoteStyle {
		LITERAL_STRING,
		BAREWORD,
		LITERAL_WORD,
		LITERAL_PROCEDURE,
		PERCENT_COMMENT,
		SHEBANG_COMMENT,
		HASH_COMMENT,
		EOF,
	}
	
	QuoteStyle quoteStyle;
	String text;
	String sourceFilename;
	int sourceLineNumber; // 1-based
	int sourceColumnNumber; // 1-based
	int sourceEndLineNumber; // Of the first character *after the end of the token*...
	int sourceEndColumnNumber; // ...to make length arithmetic simple
	
	public Token(QuoteStyle qs, String text, String fn, int ln0, int cn0, int ln1, int cn1) {
		this.quoteStyle = qs;
		this.text = text;
		this.sourceFilename   = fn;
		this.sourceLineNumber = ln0;
		this.sourceColumnNumber = cn0;
		this.sourceEndLineNumber = ln1;
		this.sourceEndColumnNumber = cn1;
	}
	
	public String getText() { return text; }
	public QuoteStyle getQuoteStyle() { return quoteStyle; }
	@Override public String getSourceFilename() { return sourceFilename; }
	@Override public int getSourceLineNumber() { return sourceLineNumber; }
	@Override public int getSourceColumnNumber() { return sourceColumnNumber; }
	@Override public int getSourceEndLineNumber() { return sourceEndLineNumber; }
	@Override public int getSourceEndColumnNumber() { return sourceEndColumnNumber; }
	
	protected static String quote(String text) {
		return "\""+text.replace("\"", "\\\"").replace("\\","\\\\")+"\"";
	}
	
	public String toString(String sep, String ind) {
		return
			"Token {"+sep+
			ind+"text: "+quote(text)+","+sep+
			ind+"quoteStyle: "+quoteStyle.name()+","+sep+
			ind+"sourceLocation: "+quote(
					sourceFilename+":"+sourceLineNumber+","+sourceColumnNumber+"-"+sourceEndLineNumber+","+sourceEndColumnNumber
				)+sep+
			"}";
	}
	
	@Override
	public String toString() {
		return toString(" ","");
	}
	
	@Override
	public boolean equals(Object oth) {
		if( !(oth instanceof Token) ) return false;
		Token other = (Token)oth;
		return
			text.equals(other.getText()) &&
			quoteStyle.equals(other.getQuoteStyle()) &&
			SourceLocations.sourceLocationsAreEqual(this, other);
	}
}
