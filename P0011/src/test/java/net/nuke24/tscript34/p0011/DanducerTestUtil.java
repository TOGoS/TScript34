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
	
	static class ArrayChunkerator<T> implements Chunkerator<T[]> {
		final T[] arr;
		final int offset;
		public ArrayChunkerator(T[] arr, int offset) {
			this.arr = arr;
			this.offset = offset;
		}
		public ArrayChunkerator(T[] arr) {
			this(arr, 0);
		}
		
		@Override
		public Chunk<T[]> next(int count) {
			count = Math.min(arr.length - offset, count);
			return new BasicChunk<T[]>(
				ArrayUtil.slice(arr, offset, count),
				new ArrayChunkerator<T>(this.arr, this.offset+count),
				offset + count == arr.length
			);
		}
		@Override public Chunkerator<T[]> prepend(T[] head) {
			if(head.length == 0) return this;
			
			return new ArrayChunkerator<T>(ArrayUtil.join(head, ArrayUtil.slice(this.arr, this.offset)));
		}
		@Override public int size() { return arr.length - offset; }

		@Override public int sizeOf(T[] t) { return t.length; }
	}
	
	public static <IS,O> DucerData<IS,O[]> processRandomlyChunked(
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
			s = s.process(chunk.getHead(), endOfInput && chunk.isEnd());
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
	
	public static <O>
	DucerData<CharSequence, O[]>
	processRandomlyChunked( DucerData<CharSequence, O[]> s, CharSequence input, boolean endOfInput, int seed, PrintStream debugStream ) {
		return processRandomlyChunked(s, new CharSequenceChunkerator(input), endOfInput, seed, debugStream);
	}
}
