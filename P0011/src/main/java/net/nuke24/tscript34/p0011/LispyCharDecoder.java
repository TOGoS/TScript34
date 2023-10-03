package net.nuke24.tscript34.p0011;

import net.nuke24.tscript34.p0011.Tokenizer.CharDecoder;

public class LispyCharDecoder implements CharDecoder {
	static final int MODE_INIT = Tokenizer.MODE_DEFAULT;
	static final int MODE_DELIMITER = 1;
	static final int MODE_BAREWORD = 2;
	static final int MODE_LINE_COMMENT = 3;
	static final int MODE_QUOTED = 4;
	static final int MODE_QUOTED_ESCAPE = 5;
	static final int MODE_END = Tokenizer.MODE_END;
	
	// INIT mode ops
	static final int[] OPS_SKIP = new int[] { Tokenizer.OP_NEXT_CHAR };
	static final int[] OPS_SKIP_TO_INIT = Tokenizer.compileOps(new String[] {
		"next-char",
		"mode = "+MODE_INIT,
	});
	static final int[] OPS_HANDLE_DELIMITER = Tokenizer.compileOps(new String[] {
		"mode = "+MODE_DELIMITER
	});
	static final int[] OPS_HANDLE_QUOTE = Tokenizer.compileOps(new String[] {
		"next-char",
		"mode = "+MODE_QUOTED,
	});
	static final int[] OPS_BAREWORD = Tokenizer.compileOps(new String[] {
		"mode = "+MODE_BAREWORD,
	});
	static final int[] OPS_HANDLE_LINE_COMMENT = Tokenizer.compileOps(new String[] {
		"next-char",
		"mode = "+MODE_LINE_COMMENT,
	});
	
	static final int[] OPS_APPEND_CHAR = Tokenizer.compileOps(new String[] {
		"buffer.append current-char",
		"next-char",
	});
	static final int[] OPS_APPEND_CHAR_AND_RETURN_TO_QUOTED = Tokenizer.compileOps(new String[] {
		"buffer.append current-char",
		"mode = "+MODE_QUOTED,
		"next-char",
	});
	static final int[] OPS_BAREWORD_TO_DELIMITER = Tokenizer.compileOps(new String[] {
		"flush-token",
		"mode = "+MODE_DELIMITER,
	});
	static final int[] OPS_END_BAREWORD = Tokenizer.compileOps(new String[] {
		"flush-token",
		"mode = "+MODE_INIT,
	});
	
	static final int[] OPS_DELIMITER_CHAR = Tokenizer.compileOps(new String[] {
		"buffer.append current-char",
		"next-char",
		"flush-token",
		"mode = "+MODE_INIT,
	});
	
	static final int[] OPS_HANDLE_QUOTED_ESCAPE = Tokenizer.compileOps(new String[] {
		"mode = "+MODE_QUOTED_ESCAPE,
		"next-char",
	});
	static final int[] OPS_HANDLE_END_QUOTE = Tokenizer.compileOps(new String[] {
		"flush-token",
		"next-char",
		"mode = "+MODE_INIT,
	});
	
	static final int[] OPS_END = Tokenizer.compileOps(new String[] {
		"mode = "+MODE_END,
	});
	
	public int[] decode(int mode, int character) {
		switch(mode) {
		case MODE_INIT:
			switch( character ) {
			case '#':
				return OPS_HANDLE_LINE_COMMENT;
			case ' ': case '\t': case '\r': case '\n':
				return OPS_SKIP;
			case '(': case ')':
				return OPS_HANDLE_DELIMITER;
			case '"':
				return OPS_HANDLE_QUOTE;
			case -1:
				return OPS_END;
			default:
				return OPS_BAREWORD;
			}
		case MODE_BAREWORD:
			switch( character ) {
			case ' ': case '\t': case '\r': case '\n':
			case '(': case ')':
			case '"':
			case -1:
				return OPS_END_BAREWORD;
			default:
				return OPS_APPEND_CHAR;
			}
		case MODE_QUOTED:
			switch( character ) {
			case '"':
				return OPS_HANDLE_END_QUOTE;
			case '\\':
				return OPS_HANDLE_QUOTED_ESCAPE;
			case -1:
				throw new RuntimeException("Encountered EOF in quoted string");
			default:
				return OPS_APPEND_CHAR;
			}
		case MODE_QUOTED_ESCAPE:
			switch( character ) {
			case '"': case '\\':
				return OPS_APPEND_CHAR_AND_RETURN_TO_QUOTED;
			default:
				throw new RuntimeException("Unrecognized escape sequence: \"\\"+(char)character+"\"");
			}
		case MODE_LINE_COMMENT:
			switch( character ) {
			case '\n':
				return OPS_SKIP_TO_INIT;
			case -1:
				return OPS_END;
			default:
				return OPS_SKIP;
			}
		case MODE_DELIMITER:
			return OPS_DELIMITER_CHAR;
		default:
			throw new RuntimeException("Unknown mode: "+mode);
		}
	};
}
