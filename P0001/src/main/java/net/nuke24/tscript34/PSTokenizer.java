package net.nuke24.tscript34;

import java.io.IOException;
import java.io.Reader;

import net.nuke24.tscript34.Token.QuoteStyle;

public class PSTokenizer {
	final Reader reader;
	String filename;
	int lineNumber = 1;
	int columnNumber = 0;
	int currentChar = -2; // meaning 'haven't started reading' 
	int tokenStartLineNumber = -1;
	int tokenStartColumnNumber = -1;
	
	public PSTokenizer(Reader r, String filename, int lineNumber, int columnNumber) {
		this.reader = r;
		this.filename = filename;
		this.lineNumber = lineNumber;
		this.columnNumber = columnNumber-1; // First nextChar() increments it
	}
	
	protected void nextChar() throws IOException {
		if( this.currentChar == -1 ) {
			return;
		} else if( this.currentChar == '\t' ) {
			++this.columnNumber;
			this.columnNumber = ((((this.columnNumber-1) / 8) + 1) * 8) + 1;
		} else if( this.currentChar == '\n' ) {
			++this.lineNumber;
			this.columnNumber = 0;
		} else {
   		++this.columnNumber;
		}
		this.currentChar = this.reader.read();
	}
	
	protected boolean isWhitespace(int character) {
		switch( character ) {
		case ' ': case '\t': case '\r': case '\n': return true;
		default: return false;
		}
	}
	
	/**
	 * Does the given character mark the beginning of a new token,
	 * even if we're in the middle of parsing a name?
	 **/
	protected static boolean isTerminator(int character) {
		switch( character ) {
		case -1:
		case ' ': case '\t': case '\r': case '\n':
		// > The characters (,. ), <, >, [, ], {, }, /, and % are special.
		// > They delimit syntactic entities such as strings, procedure bodies,
		// > name literals, and comments.  Any of these characters terminates
		// > the entity preceding it and is not included in the entity.
		// -- PostScript language reference, 3rd edition, p27
		case '(': case ')': case '<': case '>':
		case '[': case ']': case '{': case '}': 
		case '/': case '%':
			return true;
		default:
			return false;
		}
	}
	
	protected Token mkToken(Token.QuoteStyle qs, String text) {
		return new Token(qs, text, filename, tokenStartLineNumber, tokenStartColumnNumber, lineNumber, columnNumber);
	}
	
	static char decodeBsEscaped(int escChar) {
		// PLRM, p29
		switch(escChar) {
		case 'n': return '\n';
		case 'r': return '\r';
		case 't': return '\t';
		case 'b': return '\b';
		case 'f': return '\f';
		case '\\': return '\\';
		case '(': return '(';
		case ')': return ')';
		default:
			throw new IllegalArgumentException("Not currently handling '\\"+escChar+"' escape sequence");
		}
	}
	
	protected void readToEndOfLine(Appendable into) throws IOException {
		while( currentChar != -1 && currentChar != '\n' ) {
			into.append((char)currentChar);
			nextChar();
		}
	}
	
	/**
	 * Including currentChar,
	 * keep reading until a terminator character is reached.
	 * If currentChar is already a terminator, it
	 * will be the only character added to the word.
	 **/
	protected void readToEndOfWord(Appendable into) throws IOException {
		if( isTerminator(currentChar) ) {
			into.append((char)currentChar);
			nextChar();
		} else while( !isTerminator(currentChar) ) {
			into.append((char)currentChar);
			nextChar();
		};
	}
	
	public Token readToken() throws IOException {
		while( currentChar == -2 || isWhitespace(currentChar) ) nextChar();
		
		this.tokenStartLineNumber = lineNumber;
		this.tokenStartColumnNumber = columnNumber;
		boolean isLiteralName = false;
		switch( currentChar ) {
		case -1: return mkToken(Token.QuoteStyle.EOF, "");
		case '(':
			{
				nextChar();
				StringBuilder literalText = new StringBuilder();
				while( currentChar != ')' ) {
					if( currentChar == '\\' ) {
						nextChar();
						literalText.append(decodeBsEscaped(currentChar));
					} else {
						literalText.append((char)currentChar);
					}
					nextChar();
				}
				nextChar();
				return mkToken(Token.QuoteStyle.LITERAL_STRING, literalText.toString());
			}
		case '#':
			{
				nextChar();
				Token.QuoteStyle quoteStyle;
				StringBuilder tokenText = new StringBuilder();
				if( currentChar == ' ' ) {
					quoteStyle = QuoteStyle.HASH_COMMENT;
				} else if( currentChar == '!' ) {
					quoteStyle = QuoteStyle.SHEBANG_COMMENT;
				} else {
					quoteStyle = Token.QuoteStyle.BAREWORD;
					tokenText.append("#");
					tokenText.append((char)currentChar);
				}
				nextChar();
				if( quoteStyle == Token.QuoteStyle.BAREWORD ) {
					readToEndOfWord(tokenText);
				} else {
					readToEndOfLine(tokenText);
				}
				return mkToken(quoteStyle, tokenText.toString());
			}
		case '%':
			{
				nextChar();
				StringBuilder commentText = new StringBuilder();
				readToEndOfLine(commentText);
				return mkToken(Token.QuoteStyle.HASH_COMMENT, commentText.toString());
			}
		case '/':
			isLiteralName = true;
			nextChar();
		default:
			{
				StringBuilder nameText = new StringBuilder();
				readToEndOfWord(nameText);
				return mkToken(isLiteralName ? Token.QuoteStyle.LITERAL_WORD : Token.QuoteStyle.BAREWORD, nameText.toString());
			}
		}
	}
}
