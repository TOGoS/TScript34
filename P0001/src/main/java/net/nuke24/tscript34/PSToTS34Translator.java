package net.nuke24.tscript34;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import net.nuke24.tscript34.Token.QuoteStyle;

public class PSToTS34Translator {
	protected Writer ts34Writer;
	public PSToTS34Translator(Writer ts34Writer) {
		this.ts34Writer = ts34Writer;
	}
	public void translate(Reader psReader) throws IOException {
		PSTokenizer psTokenizer = new PSTokenizer(psReader, "-", 1, 1);
		Token token;
		while( (token = psTokenizer.readToken()).quoteStyle != QuoteStyle.EOF ) {
			switch( token.getQuoteStyle() ) {
			case BAREWORD:
				// intentionally left broken
			case HASH_COMMENT: case PERCENT_COMMENT: case SHEBANG_COMMENT:
				continue;
			default:
				throw new RuntimeException("Unexpected token type: "+token.getQuoteStyle());
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		Reader reader = new InputStreamReader(System.in);
		new PSToTS34Translator(new OutputStreamWriter(System.out)).translate(reader);
	}
}
