package net.nuke24.tscript34.p0011;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer implements Danducer<CharSequence, Token[]> {
	public interface CharDecoder {
		int[] decode(int mode, int character);
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
	public static final int OP_NEXT_CHAR      = 0x0000000A; // 'consume' the current character
	public static final int OP_MARK_TOKEN_START=0x0000000B;
	public static final int MODE_DEFAULT = 0;
	public static final int MODE_END     = 0xFFFF;
	
	final int CHAR_EOF = -1;
	// Placeholder for to-be-read character
	final int CHAR_PENDING = -2;
	
	public static final int opData(int op) {
		return 0xFFFF & (op >> OPSHIFT_D);
	}
	public static final int mkDataOp(int op, int data) {
		return ((data << OPSHIFT_D) & OPMASK_D) | (op & OPMASK_I);
	}
	
	static final Pattern SET_MODE_TO_CONST_PATTERN = Pattern.compile("mode = (\\d+)");
	static final Pattern BUFFER_APPEND_PATTERN = Pattern.compile("buffer.append (\\d+|current-char)");
	
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
			} else if( (m = BUFFER_APPEND_PATTERN.matcher(line)).matches() ) {
				if( "current-char".equals(m.group(1)) ) {
					ops.add(OP_APPEND_CHAR);
				} else {
					System.err.println(line+" says to append '"+(char)Integer.parseInt(m.group(1))+"'");
					ops.add(mkDataOp(OP_APPEND_DATA, Integer.parseInt(m.group(1))));
				}
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
			} else if( "next-char".equals(line) ) {
				ops.add(OP_NEXT_CHAR);
			} else if( "mark-token-start".equals(line) ) {
				ops.add(OP_MARK_TOKEN_START);
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
	final String sourceUri;
	final int tokenStartLineIndex;
	final int tokenStartColumnIndex;
	final int sourceLineIndex;
	final int sourceColumnIndex;
	public Tokenizer(
		String textBuffer, int acc, int mode, CharDecoder charDecoder, PrintStream debugStream,
		String sourceUri, int tokenStartLineIndex, int tokenStartColumnIndex, int sourceLineIndex, int sourceColumnIndex
	) {
		this.textBuffer = textBuffer;
		this.acc = acc;
		this.mode = mode;
		this.charDecoder = charDecoder;
		this.debugStream = debugStream;
		this.sourceUri = sourceUri;
		this.tokenStartLineIndex = tokenStartLineIndex;
		this.tokenStartColumnIndex = tokenStartColumnIndex;
		this.sourceLineIndex = sourceLineIndex;
		this.sourceColumnIndex = sourceColumnIndex;
	}
	public Tokenizer(CharDecoder charDecoder, String sourceUri) {
		this("", 0, 0, charDecoder, null, sourceUri, -1, -1, 0, 0);
	}
	public Tokenizer(CharDecoder charDecoder) {
		this(charDecoder, null);
	}
	
	public Tokenizer withDebugStream(PrintStream debugStream) {
		return new Tokenizer(textBuffer, acc, mode, charDecoder, debugStream, sourceUri, tokenStartColumnIndex, tokenStartColumnIndex, sourceLineIndex, sourceColumnIndex);
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
		
		int prevMode = -2;
		int prevI = -2;
		while( mode != MODE_END ) {
			if( mode == prevMode && i == prevI ) {
				throw new RuntimeException("Mode="+mode+", i="+i+": infinite loop!  Maybe someone forgot a next-char");
			}
			
			prevMode = mode;
			prevI = i;
			
			if( i >= input.length() && !endOfInput ) break;
			
			int c = i >= input.length() ? CHAR_EOF : input.charAt(i);
			
			if(debugStream != null) debugStream.println("Mode="+mode+", i="+i+"; Handling "+(c == CHAR_EOF ? "EOF" : "char: '"+(char)c+"'")+" ("+sourceUri+":"+(sourceLineIndex+1)+","+sourceColumnIndex+")");
			int[] ops = this.charDecoder.decode(mode, c);
			for( int ic=0; ic >= 0 && ic < ops.length; ) {
				int op = ops[ic++];
				if(debugStream != null) debugStream.println("Doing op["+(ic-1)+"]: 0x"+Integer.toHexString(op));
				switch( op & OPMASK_I ) {
				case OP_APPEND_CHAR  :
					if( c == CHAR_PENDING ) throw new RuntimeException("Current character not yet read!");
					textBuffer += (char)c;
					break;
				case OP_APPEND_DATA  :
					textBuffer += (char)opData(op);
					break;
				case OP_BUFFER_LENGTH : acc = textBuffer.length(); break;
				case OP_JUMP_IF_NONZERO :
					if( acc != 0 ) ic = opData(op);
					break;
				case OP_JUMP_IF_ZERO :
					if( acc == 0 ) ic = opData(op);
					break;
				case OP_FLUSH_TOKEN  :
					resultTokens.add(sourceUri == null ?
						new Token(textBuffer, mode) :
						new Token(textBuffer, mode, sourceUri,
							tokenStartLineIndex, tokenStartColumnIndex,
							    sourceLineIndex,     sourceColumnIndex));
					textBuffer = "";
					break;
				case OP_MARK_TOKEN_START:
					tokenStartLineIndex = sourceLineIndex;
					tokenStartColumnIndex = sourceColumnIndex;
					break;
				case OP_SETMODE      :
					mode = opData(op);
					if(debugStream != null) debugStream.println("$ mode = "+mode);
					break;
				case OP_NEXT_CHAR:
					if(debugStream != null) debugStream.println("Character '"+(char)c+"' consumed; bumping i and column index");
					if( c == CHAR_EOF ) {
						throw new RuntimeException("You can't _consume_ EOF!");
					} else if( c == '\n' ) {
						++sourceLineIndex;
						sourceColumnIndex = 0;
					} else {
						++sourceColumnIndex;
					}
					++i;
					c = CHAR_PENDING;
					break;
				default:
					throw new RuntimeException("Bad tokenizer opcode: 0x"+Integer.toHexString(op));
				}
			}
		}
		
		return new DucerData<CharSequence, Token[]>(
			new Tokenizer(textBuffer, acc, mode, charDecoder, debugStream, sourceUri, tokenStartLineIndex, tokenStartColumnIndex, sourceLineIndex, sourceColumnIndex),
			input.subSequence(i, input.length()).toString(),
			resultTokens.toArray(EMPTY_TOKEN_LIST),
			mode == MODE_END
		);
	}
}
