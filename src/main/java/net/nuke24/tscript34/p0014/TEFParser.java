package net.nuke24.tscript34.p0014;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.nuke24.tscript34.p0010.Function;
import net.nuke24.tscript34.p0010.ducer.DucerChunk;
import net.nuke24.tscript34.p0010.ducer.DucerState;
import net.nuke24.tscript34.p0010.ducer.InputPortState;
import net.nuke24.tscript34.p0014.LLChunks.Chunk;
import net.nuke24.tscript34.p0014.LLChunks.ContentPiece;
import net.nuke24.tscript34.p0014.LLChunks.HeaderKey;
import net.nuke24.tscript34.p0014.LLChunks.HeaderValuePiece;
import net.nuke24.tscript34.p0014.LLChunks.NewEntryLine;
import net.nuke24.tscript34.p0014.LLChunks.SyntaxError;
import net.nuke24.tscript34.p0014.util.ArrayUtil;

public record TEFParser(
	State state,
	int lineNum,
	byte[] remaining,
	int remainingOffset
)
implements Function<DucerChunk<byte[]>,DucerState<byte[],Chunk[]>>
{
	public static final TEFParser INIT = new TEFParser(State.HEADER, 1, new byte[0], 0);
	
	enum State {
		// What are we ready to parse?
		NEW_ENTRY,
		HEADER,
		HEADER_OR_CONTINUATION,
		HEADER_VALUE,
		POST_HEADER_KEY, // Just past the colon; might be newline or whitespace+data
		CONTENT_BEGIN, // First line of content, which neeeds to handle initial '=' specially
		CONTENT,
	}
	
	protected static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
	protected static final byte[] LF_SEQ = new byte[]{'\n'};
	
	protected static final Pattern NEW_ENTRY_LINE_PATTERN = Pattern.compile("^=([^\\s]*)(?:$|\\s+(.*))");
	//protected static final Pattern HEADER_PATTERN = Pattern.compile("^((?:[^\\s:]|:[^\\s])):):(?:$|\\s+(.*))");
	//protected static final Pattern COMMENT_LINE_PATTERN = Pattern.compile("^#.*");
	//protected static final Pattern EMPTY_LINE_PATTERN = Pattern.compile("^$*");
	
	protected static final Charset UTF8 = Charset.forName("UTF8");
	
	static boolean isHorizontalWhitespace(int b) {
		return b == (int)' ' || b == (int)'\t'; 
	}
	static boolean isWhitespace(int b) {
		switch(b) {
		case ' ': case '\t': case '\r': case '\n': return true;
		default: return false;
		}
	}

	
	@Override
	public DucerState<byte[], Chunk[]> apply(DucerChunk<byte[]> chunk) {
		State state = this.state;
		int lineNum = this.lineNum;
		byte[] remaining = this.remaining;
		int remainingOffset = this.remainingOffset;
		final boolean isEnd = chunk.isEnd;
		// Combine current state with input
		if( chunk.payload.length > 0 ) {
			remaining = ArrayUtil.concat(remaining, remainingOffset, chunk.payload);
			remainingOffset = 0;
		}
		
		/*
		int eolIndex = -1;
		for( int i=remainingOffset; i<remaining.length; ++i ) {
			if( remaining[i] == (byte)'\n' ) {
				eolIndex = i;
				break;
			}
		}
		if( eolIndex == -1 && isEnd ) {
			eolIndex = remaining.length;
		}
		*/
		
		List<Chunk> output = new ArrayList<Chunk>();
		
		Matcher m;
		int eolIndex;
		
		parse: while( remaining.length > remainingOffset ) {
			int char0 = remaining[remainingOffset];
			
			// There's always at least one byte to look at, otherwise we quit! 
			switch( state ) {
			case NEW_ENTRY:
				assert char0 == '=';
				eolIndex = ArrayUtil.find(remaining, remainingOffset, LF_SEQ, isEnd);
				if( eolIndex == -1 && isEnd ) eolIndex = remaining.length;
				else if( eolIndex < 0 ) break parse;
				String line = new String(remaining, remainingOffset, eolIndex-remainingOffset, UTF8);
				m = NEW_ENTRY_LINE_PATTERN.matcher(line);
				assert m.matches();
				
				output.add(NewEntryLine.of(m.group(1), m.group(2)));
				
				remainingOffset = Math.min(remaining.length, eolIndex+1);
				++lineNum;
				state = State.HEADER;
				continue parse;
			case HEADER: case HEADER_OR_CONTINUATION:
				// At beginning of a line; may be a header, a header continuation, a comment, or a blank line
				if( isHorizontalWhitespace(char0) ) {
					if( state == State.HEADER ) {
						output.add(new SyntaxError(lineNum, "Illegal header continuation line"));
					}
					// But continue anyway, I guess (TODO: bad; fix)
					// Continuation!  Add the newline
					output.add(new HeaderValuePiece("\n"));
					++remainingOffset;
					state = State.HEADER_VALUE;
					continue parse;
				} else if( char0 == '=' ) {
					if( remaining.length >= remainingOffset+2 ) {
						if( remaining[remainingOffset+1] == '=' ) {
							// It is escaped!  Skip the character
							// and continue parsing the header.
							++remainingOffset;
						} else if( !isEnd && remaining.length == remainingOffset+1 ) {
							// Ambiguity; may be escaped or not!
							break parse;
						} else {
							state = State.NEW_ENTRY;
							continue parse;
						}
					} else {
						// Can't know if that's an escape or not
						break parse;
					}
				} else if( char0 == '#' ) {
					int eol = ArrayUtil.find(remaining, remainingOffset, LF_SEQ, isEnd);
					++lineNum;
					remainingOffset = eol == -1 ? remaining.length : eol+1;
					state = State.HEADER;
					continue parse;
				} else if( char0 == '\n' ) {
					++lineNum;
					++remainingOffset;
					state = State.CONTENT_BEGIN;
					continue parse;
				}
				
				// Find the colon
				for( int i=remainingOffset; i<remaining.length; ++i ) {
					if( remaining[i] == ':' ) {
						if(
							(remaining.length >= i+2 && isWhitespace(remaining[i+1])) ||
							(remaining.length == i+1 && isEnd)
						) {
							// Found the end of the header key!
							output.add(new HeaderKey(new String(remaining, remainingOffset, i-remainingOffset, UTF8)));
							remainingOffset = i+1;
							state = State.POST_HEADER_KEY;
							continue parse;
						}
					} else if( remaining[i] == '\n' ) {
						output.add(new SyntaxError(lineNum, "Improperly terminated header key"));
						++lineNum;
						remainingOffset = i+1;
						continue parse;
					}
				}
				// If we got here, then we didn't find the end of the key.
				if( isEnd ) {
					output.add(new SyntaxError(lineNum, "Improperly terminated header key"));
					remainingOffset = remaining.length;
					break parse;
				}
				// Better luck next time.
				break parse;
			case POST_HEADER_KEY:
				if( char0 == '\n' ) {
					++lineNum;
					++remainingOffset;
					state = State.HEADER_OR_CONTINUATION;
					continue parse;
				} else {
					++remainingOffset;
					state = State.HEADER_VALUE;
					continue parse;
				}
			case HEADER_VALUE:
				eolIndex = ArrayUtil.find(remaining, remainingOffset, LF_SEQ, isEnd);
				if( eolIndex < 0 && !isEnd ) break parse;
				// Preceding whitespace already skipped;
				// "\n" + everything from here to EOL is more value
				if( eolIndex == -1 ) eolIndex = remaining.length;
				if( eolIndex - remainingOffset > 0 ) output.add(new HeaderValuePiece(new String(remaining, remainingOffset, eolIndex-remainingOffset, UTF8)));
				
				++lineNum;
				remainingOffset = Math.min(remaining.length, eolIndex+1);
				state = State.HEADER_OR_CONTINUATION;
				continue parse;
			case CONTENT_BEGIN:
				if( char0 == '=' ) {
					// Tricky special case for beginning of content!
					int nextChar = remaining.length > remainingOffset+2 ? remaining[remainingOffset+1] : isEnd ? -1 : -2;
					if( nextChar == -2 ) break parse;
					if( nextChar == '=' ) {
						// It's escaped!  Skip the first one
						// and treat the rest of the line as content.
						++remainingOffset;
					} else {
						// It's not escaped!
						state = State.NEW_ENTRY;
						continue parse;
					}
				}
				state = State.CONTENT;
				continue parse;
			case CONTENT:
				// Otherwise, start reading data
				int eofPseudoChar = isEnd ? -1 : -2;
				for( int i=remainingOffset; i<remaining.length; ++i ) {
					char0 = remaining[i];
					if( char0 == '\n' ) {
						int char1 = remaining.length >= i+2 ? remaining[i+1] : eofPseudoChar;
						int char2 = remaining.length >= i+3 ? remaining[i+2] : eofPseudoChar;
						checkForEnd: switch( char1 ) {
						case -2:
							// "\n" + ?; need more info
							remainingOffset = i;
							break parse;
						case '=':
							switch( char2 ) {
							case -2:
								// "\n=" + ?; Need more info
								remainingOffset = i;
								break parse;
							case '=':
								// "\n=="
								// Eat the newline and output the chunk
								++i;
								++lineNum;
								output.add(new ContentPiece(Arrays.copyOfRange(remaining, remainingOffset, i)));
								// Let CONTENT_BEGIN handle it from here
								remainingOffset = i;
								state = State.CONTENT_BEGIN;
								continue parse;
							default:
								// Everything else means the entry ends before the newline preceding the '='
								output.add(new ContentPiece(Arrays.copyOfRange(remaining, remainingOffset, i)));
								++lineNum;
								remainingOffset = i+1;
								state = State.NEW_ENTRY;
								continue parse;
							}
							
						default:
							// "\n" + something other than '='; nothing to see here except a new line
							break checkForEnd;
						}
						
						++lineNum;
					}
				}
				// Then we made it all the way to the end of `remaining`!
				output.add(new ContentPiece(Arrays.copyOfRange(remaining, remainingOffset, remaining.length)));
				remainingOffset = remaining.length;
				break parse;
			}
		}
		
		return new DucerState<>(
			new TEFParser(state, lineNum, remaining, remainingOffset),
			new InputPortState<byte[]>(isEnd, EMPTY_BYTE_ARRAY),
			new DucerChunk<Chunk[]>(output.toArray(new Chunk[output.size()]), isEnd)
		);
	}
}
