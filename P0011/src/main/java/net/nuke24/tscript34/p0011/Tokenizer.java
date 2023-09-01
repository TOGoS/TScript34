package net.nuke24.tscript34.p0011;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer implements Danducer<CharSequence, Tokenizer.Token[]> {
	public interface CharDecoder {
		int[] decode(int mode, int character);
	}
	
	public static class Token {
		public final String text;
		public final int mode;
		public final String sourceFilename;
		public final int sourceLineIndex;
		public final int sourceColumnIndex;
		public final int sourceEndLineIndex;
		public final int sourceEndColumnIndex;
		public Token(
			String text, int mode,
			String sourceFilename,
			int sourceLineIndex, int sourceColumnIndex,
			int sourceEndLineIndex, int sourceEndColumnIndex
		) {
			this.text = text;
			this.mode = mode;
			this.sourceFilename = sourceFilename;
			this.sourceLineIndex = sourceLineIndex;
			this.sourceColumnIndex = sourceColumnIndex;
			this.sourceEndLineIndex = sourceEndLineIndex;
			this.sourceEndColumnIndex = sourceEndColumnIndex;
		}
		public Token(String text, int mode) {
			this(text, mode, null, -1, -1, -1, -1);
		}
		@Override public boolean equals(Object obj) {
			if( !(obj instanceof Token) ) return false;
			Token ot = (Token)obj;
			return text.equals(ot.text) && mode == ot.mode &&
				(this.sourceFilename == ot.sourceFilename ||
				(this.sourceFilename != null && this.sourceFilename.equals(ot.sourceFilename))) &&
				this.sourceLineIndex == ot.sourceLineIndex &&
				this.sourceColumnIndex == ot.sourceColumnIndex &&
				this.sourceEndLineIndex == ot.sourceEndLineIndex &&
				this.sourceEndColumnIndex == ot.sourceEndColumnIndex;
		}
		
		@Override public int hashCode() {
			return text.hashCode() ^ mode +
				(this.sourceFilename == null ? 0 : this.sourceFilename.hashCode()) +
				(this.sourceLineIndex      <<  0) +
				(this.sourceColumnIndex    <<  8) +
				(this.sourceEndLineIndex   << 16) +
				(this.sourceEndColumnIndex << 24);
		}
		
		@Override public String toString() {
			String slocStr = (
				this.sourceFilename == null &&
				sourceLineIndex == -1 && sourceColumnIndex == -1 &&
				sourceEndLineIndex == -1 && sourceEndColumnIndex == -1
			) ? "" : ", \""+
				sourceFilename+":"+
				(sourceLineIndex+1)+","+(sourceColumnIndex+1)+".."+
				(sourceEndLineIndex+1)+","+(sourceEndColumnIndex+1)+"\"";
			return "Token(\"" + text + "\", "+mode+slocStr+")";
		}
	}
	
	static final Token[] EMPTY_TOKEN_LIST = new Token[0];
	static final int[] EMPTY_OP_LIST = new int[0];
	
	public static final int OPMASK_I          = 0x0000FFFF;
	public static final int OPMASK_D          = 0xFFFF0000;
	public static final int OPSHIFT_D         = 16;
	public static final int OP_FLUSH_TOKEN    = 0x00000001;
	public static final int OP_BUFFER_LENGTH  = 0x00000002;
	public static final int OP_INVERT         = 0x00000003;
	public static final int OP_JUMP_IF_ZERO   = 0x00000004;
	public static final int OP_JUMP_IF_NONZERO= 0x00000005;
	public static final int OP_APPEND_CHAR    = 0x00000006; // Append input char to text buffer
	public static final int OP_APPEND_DATA    = 0x00000007; // Append op data to text buffer
	public static final int OP_SETMODE        = 0x00000008; // Set mode to op >> 16; mode 0xFFFF means 'end'
	public static final int OP_REJECT_CHAR    = 0x00000009; // Put character back into input queue and try handling again (presumably with a different mode)
	public static final int MODE_DEFAULT = 0;
	public static final int MODE_END     = 0xFFFF;
	
	public static final int opData(int op) {
		return 0xFFFF & (op >> OPSHIFT_D);
	}
	public static final int mkDataOp(int op, int data) {
		return ((data << OPSHIFT_D) & OPMASK_D) | (op & OPMASK_I);
	}
	
	static final Pattern SET_MODE_TO_CONST_PATTERN = Pattern.compile("mode = (\\d+)");
	static final Pattern JUMP_TO_MODE_TO_CONST_PATTERN = Pattern.compile("jump-to-mode (\\d+)");
	
	public static int[] compileOps(String[] asm) {
		ArrayList<Integer> ops = new ArrayList<Integer>();
		ArrayList<Integer> fixups = new ArrayList<Integer>();
		Matcher m;
		for( int i=0; i<asm.length; ++i ) {
			String line = asm[i].trim();
			if( line.startsWith("#") || line.isEmpty() ) {
				continue;
			} else if( "acc = buffer.length".equals(line) ) {
				ops.add(OP_BUFFER_LENGTH);
			} else if( "buffer.append current-char".equals(line) ) {
				ops.add(OP_APPEND_CHAR);
			} else if( "flush-token".equals(line) ) {
				ops.add(OP_FLUSH_TOKEN);
			} else if( "if acc != 0 {".equals(line) ) {
				fixups.add(ops.size());
				ops.add(OP_JUMP_IF_ZERO);
			} else if( "if acc == 0 {".equals(line) ) {
				fixups.add(ops.size());
				ops.add(OP_JUMP_IF_NONZERO);
			} else if( "}".equals(line) ) {
				int fixupPos = fixups.remove(fixups.size()-1);
				ops.set(fixupPos, mkDataOp(ops.get(fixupPos).intValue(), ops.size()));
			} else if( "if acc == 0 {".equals(line) ) {
				fixups.add(ops.size());
				ops.add(OP_JUMP_IF_NONZERO);
			} else if( "reject-char".equals(line) ) {
				ops.add(OP_REJECT_CHAR);
			} else if( (m = JUMP_TO_MODE_TO_CONST_PATTERN.matcher(line)).matches() ) {
				ops.add(mkDataOp(OP_SETMODE, Integer.parseInt(m.group(1))));
				ops.add(OP_REJECT_CHAR);
			} else if( (m = SET_MODE_TO_CONST_PATTERN.matcher(line)).matches() ) {
				ops.add(mkDataOp(OP_SETMODE, Integer.parseInt(m.group(1))));
			} else {
				throw new RuntimeException("Unrecognized tokenizer op string: "+line);
			}
		}
		int[] opArr = new int[ops.size()];
		for( int i=0; i<opArr.length; ++i ) {
			opArr[i] = ops.get(i);
		}
		return opArr;
	}
	
	
	final String textBuffer;
	final int acc;
	final int mode;
	final CharDecoder charDecoder;
	final PrintStream debugStream;
	final String sourceFilename;
	final int tokenStartLineIndex;
	final int tokenStartColumnIndex;
	final int sourceLineIndex;
	final int sourceColumnIndex;
	public Tokenizer(
		String textBuffer, int acc, int mode, CharDecoder charDecoder, PrintStream debugStream,
		String sourceFilename, int tokenStartLineIndex, int tokenStartColumnIndex, int sourceLineIndex, int sourceColumnIndex
	) {
		this.textBuffer = textBuffer;
		this.acc = acc;
		this.mode = mode;
		this.charDecoder = charDecoder;
		this.debugStream = debugStream;
		this.sourceFilename = sourceFilename;
		this.tokenStartLineIndex = tokenStartLineIndex;
		this.tokenStartColumnIndex = tokenStartColumnIndex;
		this.sourceLineIndex = sourceLineIndex;
		this.sourceColumnIndex = sourceColumnIndex;
	}
	public Tokenizer(CharDecoder charDecoder, String sourceFilename) {
		this("", 0, 0, charDecoder, null, sourceFilename, -1, -1, 0, 0);
	}
	public Tokenizer(CharDecoder charDecoder) {
		this(charDecoder, null);
	}
	
	public Tokenizer withDebugStream(PrintStream debugStream) {
		return new Tokenizer(textBuffer, acc, mode, charDecoder, debugStream, sourceFilename, tokenStartColumnIndex, tokenStartColumnIndex, sourceLineIndex, sourceColumnIndex);
	}
	public Tokenizer withSourceLocation(String sourceFilename, int sourceLineIndex, int sourceColumnIndex) {
		return new Tokenizer(textBuffer, acc, mode, charDecoder, debugStream, sourceFilename, tokenStartColumnIndex, tokenStartColumnIndex, sourceLineIndex, sourceColumnIndex);
	}
	
	@Override
	public DucerData<CharSequence, Token[]>
	process(CharSequence input, boolean endOfInput) {
		int acc = this.acc;
		int mode = this.mode;
		int tokenStartLineIndex   = this.tokenStartLineIndex;
		int tokenStartColumnIndex = this.tokenStartColumnIndex;
		int sourceLineIndex       = this.sourceLineIndex;
		int sourceColumnIndex     = this.sourceColumnIndex;
		String textBuffer = this.textBuffer;
		ArrayList<Token> resultTokens = new ArrayList<Token>();
				
		int i=0;
		loop_over_chars: while( mode != MODE_END ) {
			if( i >= input.length() && !endOfInput ) break;
			
			int c = i >= input.length() ? -1 : input.charAt(i);
			// A slight hack so that ops don't need to explicitly indicate
			// where token end char should be; if a character is appended
			// during processing, assume that the end of the token should be
			// *after* this character, rather than before it.
			boolean charAppended = false;
			boolean charConsumed = c != -1; // You can't 'consume' EOF
			
			if(debugStream != null) debugStream.println("Mode="+mode+", i="+i+"; Handling "+(c == -1 ? "EOF" : "char: '"+(char)c+"'")+" ("+sourceFilename+":"+(sourceLineIndex+1)+","+sourceColumnIndex+"); by default, charConsumed="+charConsumed);
			int[] ops = this.charDecoder.decode(mode, c);
			for( int ic=0; ic >= 0 && ic < ops.length; ) {
				int op = ops[ic++];
				if(debugStream != null) debugStream.println("Doing op["+(ic-1)+"]: 0x"+Integer.toHexString(op));
				switch( op & OPMASK_I ) {
				case OP_APPEND_CHAR  :
					textBuffer += (char)c;
					charAppended = true;
					break;
				case OP_APPEND_DATA  :
					textBuffer += (char)opData(op);
					charAppended = true;
					break;
				case OP_BUFFER_LENGTH : acc = textBuffer.length(); break;
				case OP_JUMP_IF_NONZERO :
					if( acc != 0 ) ic = opData(op);
					break;
				case OP_JUMP_IF_ZERO :
					if( acc == 0 ) ic = opData(op);
					break;
				case OP_FLUSH_TOKEN  :
					resultTokens.add(sourceFilename == null ?
						new Token(textBuffer, mode) :
						new Token(textBuffer, mode, sourceFilename,
							tokenStartLineIndex, tokenStartColumnIndex,
							sourceLineIndex, sourceColumnIndex+(charAppended?1:0))); // Should really use a separate 'post char position', since what if c == '\n'?
					textBuffer = "";
					break;
				case OP_SETMODE      :
					mode = opData(op);
					tokenStartLineIndex = sourceLineIndex;
					tokenStartColumnIndex = sourceColumnIndex;
					if(debugStream != null) debugStream.println("$ mode = "+mode);
					break;
				case OP_REJECT_CHAR  :
					charConsumed = false;
					if(debugStream != null) debugStream.println("$ reject-char; i="+i+", charConsumed="+charConsumed);
					continue loop_over_chars;
				default:
					throw new RuntimeException("Bad tokenizer opcode: 0x"+Integer.toHexString(op));
				}
			}
			
			if( charConsumed ) {
				++i;
				if(debugStream != null) debugStream.println("Character '"+(char)c+"' consumed; bumping i and column index");
				if( c == '\n' ) {
					++sourceLineIndex;
					sourceColumnIndex = 0;
				} else {
					++sourceColumnIndex;
				}
			}
		}
		
		return new DucerData<CharSequence, Token[]>(
			new Tokenizer(textBuffer, acc, mode, charDecoder, debugStream, sourceFilename, tokenStartLineIndex, tokenStartColumnIndex, sourceLineIndex, sourceColumnIndex),
			input.subSequence(i, input.length()).toString(),
			resultTokens.toArray(EMPTY_TOKEN_LIST),
			mode == MODE_END
		);
	}
}
