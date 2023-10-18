package net.nuke24.tscript34.p0011;

import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Random;

import net.nuke24.tscript34.p0011.Danducer.DucerData;

public class DanducerTestUtil {
	public static interface Chunkerator<T> {
		Chunk<T> next(int count);
		int size();
		Chunkerator<T> prepend(T head);
		int sizeOf(T t);
	}
	public static interface Chunk<T> {
		T getHead();
		Chunkerator<T> getTail();
		boolean isEnd();
	}
	public static class BasicChunk<T> implements Chunk<T> {
		final T head;
		final Chunkerator<T> tail;
		boolean isEnd;
		BasicChunk(T head, Chunkerator<T> tail, boolean isEnd) {
			this.head = head;
			this.tail = tail;
			this.isEnd = isEnd;
		}
		@Override public T getHead() { return head; }
		@Override public Chunkerator<T> getTail() { return tail; }
		@Override public boolean isEnd() { return isEnd; }
		@Override public String toString() {
			String headStr =
				head instanceof CharSequence ? "\"" +
					head.toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n","\\n") + "\"" :
				head.toString();
			return "BasicChunk("+headStr+", "+tail.size()+" items remaining)";
		}
	}
	
	public static <IS,O> DucerData<IS,O[]> processChunked(
		DucerData<IS, O[]> s,
		Chunkerator<IS> input,
		boolean endOfInput, int seed, PrintStream debugStream
	) {
		if(debugStream != null) debugStream.println();
		Random r = new Random(seed);
		ArrayList<O> outputs = new ArrayList<O>();
		while( input.size() > 0 ) {
			int chunkLen = Math.max(1, Math.min(input.size(), r.nextInt(4)));
			Chunk<IS> chunk = input.next(chunkLen);
			if(debugStream != null) debugStream.println("Chunk: \""+chunk+"\"");
			s = s.process(chunk.getHead(), chunk.isEnd());
			for( int i=0; i<s.output.length; ++i ) {
				outputs.add(s.output[i]);
			}
			input = chunk.getTail().prepend(s.remainingInput);
		}
		@SuppressWarnings("unchecked")
		O[] outputArr = (O[])Array.newInstance(s.output.getClass().getComponentType(), outputs.size());
		return new DucerData<IS, O[]>(
			s.state, s.remainingInput,
			outputs.toArray(outputArr),
			s.isDone
		);
	}
	
	static class CharSequenceChunkerator implements Chunkerator<CharSequence> {
		final CharSequence cs;
		final int offset;
		public CharSequenceChunkerator(CharSequence cs, int offset) {
			this.cs = cs;
			this.offset = offset;
		}
		public CharSequenceChunkerator(CharSequence cs) {
			this(cs, 0);
		}
		
		@Override
		public Chunk<CharSequence> next(int count) {
			count = Math.min(cs.length() - offset, count);
			return new BasicChunk<CharSequence>(
				cs.subSequence(offset, offset+count),
				new CharSequenceChunkerator(cs, offset+count),
				offset + count == cs.length()
			);
		}
		@Override public Chunkerator<CharSequence> prepend(CharSequence head) {
			if(head.length() == 0) return this;
			
			StringBuilder sb = new StringBuilder();
			sb.append(head);
			sb.append(cs.subSequence(offset, cs.length()));
			
			return new CharSequenceChunkerator(sb);
		}
		@Override public int size() { return cs.length() - offset; }

		@Override public int sizeOf(CharSequence t) { return t.length(); }
	}
	
	static boolean useChunkerator = true;
	
	public static <O>
	DucerData<CharSequence, O[]>
	processChunked( DucerData<CharSequence, O[]> s, CharSequence input, boolean endOfInput, int seed, PrintStream debugStream ) {
		if(useChunkerator) return processChunked(s, new CharSequenceChunkerator(input), endOfInput, seed, debugStream);
		
		if(debugStream != null) debugStream.println();
		Random r = new Random(seed);
		StringBuilder remainingInput = new StringBuilder();
		ArrayList<O> outputs = new ArrayList<O>();
		int offset=0;
		while( offset < input.length() ) {
			int chunkLen = Math.max(1, Math.min(input.length() - offset, r.nextInt(4)));
			int chunkEnd = offset+chunkLen;
			boolean chunkIsEndOfInput = endOfInput && chunkEnd == input.length();
			CharSequence chunk = input.subSequence(offset, chunkEnd);
			if(debugStream != null) debugStream.println("Chunk: \""+chunk+"\"");
			s = s.process(chunk, chunkIsEndOfInput);
			for( int i=0; i<s.output.length; ++i ) {
				outputs.add(s.output[i]);
			}
			offset = chunkEnd;
		}
		@SuppressWarnings("unchecked")
		O[] outputArr = (O[])Array.newInstance(s.output.getClass().getComponentType(), outputs.size());
		return new DucerData<CharSequence, O[]>(
			s.state, remainingInput.toString(),
			outputs.toArray(outputArr),
			s.isDone
		);
	}
}
