// -*- insert-tabs-mode: t; tab-width: 3; c-basic-offset: 3 -*-

using ArgumentException = System.ArgumentException;
using Console = System.Console;
using DefDict = System.Collections.Generic.Dictionary<string, object>;
using Exception = System.Exception;
using Float32 = System.Single;
using Float64 = System.Double;
using HttpClient = System.Net.Http.HttpClient;
using IEnumerable = System.Collections.IEnumerable;
using Int32 = System.Int32;
using Int64 = System.Int64;
using IStringList = System.Collections.Generic.IList<string>;
using Object = System.Object;
using SCG = System.Collections.Generic;
using Stack = System.Collections.Generic.List<object>;
using Stream = System.IO.Stream;
using StreamReader = System.IO.StreamReader;
using StringBuilder = System.Text.StringBuilder;
using StringList = System.Collections.Generic.List<string>;
using StringSplitOptions = System.StringSplitOptions;
using TextReader = System.IO.TextReader;
using Uri = System.Uri;

namespace TOGoS.TScrpt34_2 {
	public interface Op {
		void Do(Interpreter interp);
	}
	public interface OpConstructor {
		Op Parse(IStringList args, IUriResolver resolver);
	}
	public class Procedure : Op {
		SCG.IList<Op> Operations;
		public Procedure(SCG.IList<Op> ops) {
			this.Operations = ops;
		}
		public void Do(Interpreter interp) {
			foreach(Op op in this.Operations) {
				// Maybe some way to return idk
				op.Do(interp);
			}
		}
	}
	
	public interface ICodec<V,L> {
		L Encode(V v);
		V Decode(L l);
	}
	
	public interface IEncoding : ICodec<object,object> {
		string Uri { get; }
	}

	public class DecimalEncoding : IEncoding {
		public string Uri { get { return "http://www.w3.org/2001/XMLSchema#decimal"; } }
		// TODO: Make sure this is actually following http://www.w3.org/2001/XMLSchema#decimal
		public object Encode(object v) {
			return ((double)v).ToString();
		}
		public object Decode(object l) {
			return Float64.Parse(l.ToString());
		}
	}
	public class SymbolEncoding : IEncoding {
		public string Uri { get { return "http://ns.nuke24.net/TScript34/Datatypes/Symbol"; } }
		public object Encode(object v) {
			return v;
		}
		public object Decode(object l) {
			return l;
		}
	}

	/** Since this encoder doesn't have access to the interpreter,
	 * it can only do very basic decoding, like data:,... */
	public class UriReferenceEncoding : IEncoding {
		public string Uri { get { return "http://ns.nuke24.net/TOGVM/Datatypes/URIResource"; } }

		protected IUriResolver Resolver;

		public UriReferenceEncoding(IUriResolver resolver) {
			this.Resolver = resolver;
		}

		public object Encode(object v) {
			if( v is string ) {
				return "data:,"+System.Uri.EscapeDataString((string)v);
			}
			throw new Exception("UriReferenceEncoding cannot encode a "+v.GetType());
		}
		public object Decode(object l) {
			if( l is string ) {
				return Resolver.Resolve((string)l);
			}
			throw new Exception("UriReferenceEncoding cannot decode a "+l.GetType()+" because it is not a string");
		}
	}

	public class ThunkedValueCollectionEncoding : IEncoding {
		public string Uri { get { return "http://ns.nuke24.net/TScript34/Datatypes/ThunkedValueCollection"; } }
		
		protected ICodec<object,TS34Thunk> ThunkCodec;
		public ThunkedValueCollectionEncoding(ICodec<object,TS34Thunk> thunkCodec) {
			this.ThunkCodec = thunkCodec;
		}
		
		public object Encode(object v) {
			throw new Exception(GetType()+"#Encode not yet implemented (usually they are created directly by e.g. createArray ops)");
		}
		public object Decode(object collection) {
			if( collection is SCG.IDictionary<object,TS34Thunk> ) {
				SCG.Dictionary<object,object> decoded = new SCG.Dictionary<object,object>();
				foreach( var entry in (SCG.IDictionary<object,TS34Thunk>)collection ) {
					decoded[entry.Key] = this.ThunkCodec.Decode(entry.Value);
				}
				return decoded;
			} else if( collection is SCG.IDictionary<object,object> ) {
				throw new Exception("Object is a regular dictionary; can't thunk-decode elements");
			} else if( collection is SCG.IList<TS34Thunk> ) {
				SCG.List<object> decoded = new SCG.List<object>();
				foreach( var item in (SCG.IList<TS34Thunk>)collection ) {
					decoded.Add(this.ThunkCodec.Decode(item));
				}
				return decoded;
			} else if( collection is SCG.IList<object> ) {
				throw new Exception("Object is a regular list; can't thunk-decode elements");
			} else {
				throw new Exception("Don't know how to ThunkedValueCollection-decode a "+collection.GetType());
			}
		}
	}

	public class QuitException : Exception {}
	
	public class Mark {
		private Mark() { }
		// Mark and other purely symbolic things don't need instances
		//public static Mark Instance = new Mark();
		public static TS34Thunk Thunk = TS34Thunk.ForUri("http://ns.nuke24.net/TScript34/Values/Mark");
	}
	
	public interface IUriResolver {
		object Resolve(string uri);
	}

	class AUriResolver : IUriResolver {
		object IUriResolver.Resolve(string uri) {
			if( uri.StartsWith("data:,") ) {
				return Uri.UnescapeDataString(uri.Substring(6));
			} else if( uri.StartsWith("http:") || uri.StartsWith("https:") ) {
				using (HttpClient client = new HttpClient()) {
					// For now just strings
					string s = client.GetStringAsync(uri).Result;
					return s;
				}
			} else {
				return null;
			}
		}
	}

	class UriResource {
		public readonly string Uri;
		IUriResolver resolver;
		#nullable enable
		object? cachedValue;
		
		public UriResource(string uri, IUriResolver resolver) {
			this.Uri = uri;
			this.resolver = resolver;
		}

		public object Get() {
			if( this.cachedValue == null ) {
				this.cachedValue = this.resolver.Resolve(this.Uri);
			}
			return this.cachedValue;
		}
		
		public string ToUriString() {
			return "<"+this.Uri+">";
		}
		public string ToContentString() {
			return this.Get().ToString();
		}
		public override string ToString() {
			return this.ToContentString();
		}
		#nullable disable
	}
	
	class AliasOp : Op {
		string name;
		string targetName;
		public AliasOp(string name, string targetName) {
			this.name = name;
			this.targetName = targetName;
		}
		void Op.Do(Interpreter interp) {
			object value = ((IUriResolver)interp).Resolve(this.targetName);
			if( value == null ) {
				throw new Exception("alias: '"+this.targetName+"' not defined");
			}
			interp.Definitions[this.name] = value;
		}
	}
	class AliasOpConstructor : OpConstructor {
		Op OpConstructor.Parse(IStringList args, IUriResolver resolver) {
			if( args.Count != 2 ) {
				throw new ArgumentException("'alias' requires exactly two arguments; new name and name of value to be aliased");
			}
			return new AliasOp(args[0], args[1]);
		}
	}

	class CountToMarkOp : Op {
		void Op.Do(Interpreter interp) {
			interp.PushValue(interp.CountToMark());
		}
	}

	/** (item1 item2 ... itemN n ArrayFromStack -- array) */
	class ArrayFromStackOp : Op {
		void Op.Do(Interpreter interp) {
			int length = interp.ThunkToValue<int>(interp.PopThunk());
			int stackOffset = interp.DataStack.Count - length;
			if( stackOffset < 0 ) {
				throw new Exception("Can't create array of "+length+" elements; only "+interp.DataStack.Count+" on stack!");
			}
			SCG.List<TS34Thunk> arr = new SCG.List<TS34Thunk>();
			arr.AddRange(interp.DataStack.GetRange(stackOffset, length));
			interp.DataStack.RemoveRange(stackOffset, length);
			interp.PushThunk(interp.ThunkedValueCollectionToThunk(arr));

			//SCG.IList<object> decoded = new SCG.List<object>(arr.Count);
			//foreach( TS34Thunk thunk in arr ) {
			//	decoded.Add(interp.ThunkToValue<object>(thunk));
			//}
			//interp.PushValue(decoded);
		}
	}
	class DefineOp : Op {
		void Op.Do(Interpreter interp) {
			TS34Thunk value = interp.PopThunk();
			string key = interp.ThunkToValue<string>(interp.PopThunk());
			interp.Definitions.Add(key.ToString(), interp.ThunkToValue<object>(value));
		}
	}
	class DictFromStackOp : Op {
		void Op.Do(Interpreter interp) {
			int length = interp.ThunkToValue<int>(interp.PopThunk());
			if( (length & 1) != 0 ) {
				throw new Exception("DictFromStackOp requires an even number of stack elements; "+length+" were indicated");
			}
			int stackOffset = interp.DataStack.Count - length;
			if( stackOffset < 0 ) {
				throw new Exception("Can't create array of "+length+" elements; only "+interp.DataStack.Count+" on stack!");
			}
			System.Collections.Generic.Dictionary<object,TS34Thunk> dict = new System.Collections.Generic.Dictionary<object,TS34Thunk>();
			for( int i=stackOffset; i<interp.DataStack.Count; i += 2 ) {
				// TODO: Translate strings to name objects as per postscript spec
				object key = interp.ThunkToValue<object>(interp.DataStack[i]);
				dict[key] = interp.DataStack[i+1];
			}
			interp.DataStack.RemoveRange(stackOffset, length);
			interp.PushThunk(interp.ThunkedValueCollectionToThunk(dict));
		}
	}
	class DupOp : Op {
		void Op.Do(Interpreter interp) {
			if( interp.DataStack.Count < 1 ) throw new Exception("Can't dup; stack empty!");
			interp.PushThunk(interp.Peek());
		}
	}
	class EncodeOpConstructor : OpConstructor {
		public static SCG.IList<IEncoding> ParseEncodings(IStringList encodingRefs, IUriResolver resolver) {
			SCG.IList<IEncoding> encodings = new SCG.List<IEncoding>();
			foreach( string encodingRef in encodingRefs ) {
				object maybeAnEncoding = resolver.Resolve(encodingRef);
				if( !(maybeAnEncoding is IEncoding) ) {
					throw new Exception(encodingRef+" does not name an encoding; is "+ValueUtil.Describe(maybeAnEncoding));
				}
				encodings.Add((IEncoding)maybeAnEncoding);
			}
			return encodings;
		}

		Op OpConstructor.Parse(IStringList args, IUriResolver resolver) {
			return new EncodeOp(ParseEncodings(args, resolver));
		}
	}
	class DecodeOpConstructor : OpConstructor {
		Op OpConstructor.Parse(IStringList args, IUriResolver resolver) {
			return new DecodeOp(EncodeOpConstructor.ParseEncodings(args, resolver));
		}
	}
	class EncodeOp : Op {
		protected SCG.IList<IEncoding> Encodings;
		public EncodeOp(SCG.IList<IEncoding> encodings) {
			this.Encodings = encodings;
		}
		public void Do(Interpreter interp) {
			// Theoretically some encodings could work on thunks.
			// e.g. if value is a think whose last encoding applied is the one to be re-applied,
			// simply make a new thunk dropping that encoding.
			object value = interp.PopValue();
			foreach( IEncoding encoding in this.Encodings ) {
				value = encoding.Encode(value);
			}
			interp.PushValue(value);
		}
	}
	class DecodeOp : Op {
		protected SCG.IList<IEncoding> Encodings;
		public DecodeOp(SCG.IList<IEncoding> encodings) {
			this.Encodings = encodings;
		}
		public void Do(Interpreter interp) {
			// Theoretically some decodings could work on thunks.
			// Just add the named encoding to the list of decode steps to be done!
			object value = interp.PopValue();
			foreach( IEncoding encoding in this.Encodings ) {
				value = encoding.Decode(value);
			}
			interp.PushValue(value);
		}
	}

	class ExchOp : Op {
		void Op.Do(Interpreter interp) {
			TS34Thunk a = interp.PopThunk();
			TS34Thunk b = interp.PopThunk();
			interp.PushThunk(a);
			interp.PushThunk(b);
		}
	}
	class ExecOp : Op {
		void Op.Do(Interpreter interp) {
			interp.ThunkToValue<Op>(interp.PopThunk()).Do(interp);
		}
	}
	class FetchUriOp : Op {
		void Op.Do(Interpreter interp) {
			string uri = interp.ThunkToValue<string>(interp.PopThunk());
			interp.PushThunk(TS34Thunk.ForUri(uri));
		}
	}
	/** i.e. take all the strings in an array and concatenate them together */
	class FlattenStringListOp : Op {
		void Op.Do(Interpreter interp) {
			IEnumerable list = interp.ThunkToValue<IEnumerable>(interp.PopThunk());
			StringBuilder sb = new StringBuilder();
			foreach( var item in list ) sb.Append(item.ToString());
			interp.PushThunk(interp.ValueToThunk(sb.ToString()));
		}
	}

	/** Equivalent to postscript's forall (list proc -- ...) */
	class ForEachOp : Op {
		void Op.Do(Interpreter interp) {
			Op proc = interp.ThunkToValue<Op>(interp.PopThunk());
			SCG.IEnumerable<TS34Thunk> list = interp.ThunkToValueShallow<SCG.IEnumerable<TS34Thunk>>(interp.PopThunk());
			// TODO: Oh dear, hold up; we probably should have a separate
			// method that returns an IEnumerable of thunks!
			foreach( TS34Thunk item in list ) {
				interp.PushThunk(item);
				proc.Do(interp);
			}
		}
	}

	/**
	 * Equivalent to postscripts 'get' (collection key -- value)
	*/
	class GetElementOp : Op {
		void Op.Do(Interpreter interp) {
			TS34Thunk keyThunk = interp.PopThunk();
			object collection = interp.ThunkToValueShallow<object>(interp.PopThunk());
			// TODO: For now assuming that any Collection<TS34Thunk> represents
			// a collection of things-by-thunk, not a collection of thunks!
			if( collection is SCG.IDictionary<object,TS34Thunk> ) {
				object key = interp.ThunkToValue<object>(keyThunk);
				interp.PushThunk( ((SCG.IDictionary<object,TS34Thunk>)collection)[key] );
			} else if( collection is SCG.IDictionary<object,object> ) {
				object key = interp.ThunkToValue<object>(keyThunk);
				interp.PushThunk( interp.ValueToThunk(((SCG.IDictionary<object,object>)collection)[key]) );
			} else if( collection is SCG.IList<TS34Thunk> ) {
				int index = interp.ThunkToValue<int>(keyThunk);
				interp.PushThunk( ((SCG.IList<TS34Thunk>)collection)[index] );
			} else if( collection is SCG.IList<object> ) {
				int index = interp.ThunkToValue<int>(keyThunk);
				interp.PushThunk( interp.ValueToThunk(((SCG.IList<object>)collection)[index]) );
			} else {
				throw new Exception("Don't know how to get element of "+collection.GetType());
			}
		}
	}
	class PopOp : Op {
		void Op.Do(Interpreter interp) {
			interp.PopThunk();
		}
	}
	class PushOp : Op {
		TS34Thunk toPush;
		public PushOp(TS34Thunk toPush) {
			this.toPush = toPush;
		}
		void Op.Do(Interpreter interp) {
			interp.PushThunk(toPush);
		}
	}
	
	class PushValueOpConstructor : OpConstructor {
		// TODO: Could have eager/lazy variants;
		// eager ones would resolve/decode immediately.
		// Possibly eager/lazy based on encoding!

		Op OpConstructor.Parse(IStringList args, IUriResolver resolver) {
			if( args.Count < 1 ) {
				throw new ArgumentException("'push-value' expects one or more arguments (value, encoding1, encoding2...)");
			}
			
			// Since our args[0] is always at least URI-encoded
			TS34EncodingList encodingList = null;
			for( int i=args.Count-1; i>0; --i ) {
				// Even if not eagerly decoding, we want to get the canonical
				// URI of the encoding.  Don't want to store some alias!
				// Also, no sense in creating thunks with invalid encodings.
				var encoding = resolver.Resolve(args[i]);
				if( !(encoding is IEncoding) ) {
					throw new Exception(args[i]+" does not name an encoding");
				}
				encodingList = new TS34EncodingList(((IEncoding)encoding).Uri, encodingList);
			}
			encodingList = new TS34EncodingList("http://ns.nuke24.net/TOGVM/Datatypes/URIResource", encodingList);
			return new PushOp(new TS34Thunk(args[0], encodingList));
		}
	}

	/**
	 * Simplified interface for a stream-to-be-written-to
	 * allowing both strings and byte arrays to be written.
	 */
	public interface ISimpleOutput {
		void Write(byte[] data);
		void Write(char[] data);
	}

	public class ConsoleOutput : ISimpleOutput {
		public void Write(byte[] data) {
			throw new Exception("ConsoleOutput can't write raw bytes");
		}
		public void Write(char[] data) {
			Console.Out.Write(data);
		}
	}
	public class StreamOutput : ISimpleOutput {
		Stream OutStream;
		public StreamOutput(Stream outstre) {
			this.OutStream = outstre;
		}
		public void Write(byte[] data) {
			// this.OutStream.Write(data); //  Could not load type 'System.ReadOnlySpan`1'
			this.OutStream.Write(data, 0, data.Length); // Works better
		}
		public void Write(char[] chars) {
			this.Write(System.Text.Encoding.ASCII.GetBytes(chars));
		}
	}
	public class ErroringOutput : ISimpleOutput {
		public void Write(byte[] data) {
			throw new Exception("No output configured");
		}
		public void Write(char[] chars) {
			throw new Exception("No output configured");
		}
	}

	public static class SimpleOutputHelper {
		public static void Write(this ISimpleOutput outputter, string data) {
			outputter.Write(data.ToCharArray());
		}
	}
	
	public interface IFormatter {
		void Format(object val, ISimpleOutput dest);
	}
	class ToStringFormatter : IFormatter {
		public static ToStringFormatter Instance = new ToStringFormatter();
		
		void IFormatter.Format(object val, ISimpleOutput dest) {
			if( val is System.Byte[] ) {
				dest.Write((byte[])val);
			} else {
				dest.Write(val.ToString());
			}
		}
	}
	class PostScriptSourceFormatter : IFormatter {
		public static PostScriptSourceFormatter Instance = new PostScriptSourceFormatter();
		
		void FormatString(string v, ISimpleOutput dest) {
			dest.Write("("+v.Replace("\\","\\\\").Replace("(","\\(").Replace(")","\\)")+")");
		}
		
		public void Format(object val, ISimpleOutput dest) {
			if( val is Int32 || val is Int64 || val is Float32 || val is Float64 ) {
				dest.Write(val.ToString());
			} else if( val is string ) {
				FormatString((string)val, dest);
			} else if( val is SCG.IDictionary<object,object> ) {
				string sep = " ";
				dest.Write("<<");
				foreach( System.Collections.Generic.KeyValuePair<object, object> pair in (SCG.IDictionary<object,object>)val ) {
					dest.Write(sep);
					((IFormatter)this).Format(pair.Key, dest);
					dest.Write(" ");
					((IFormatter)this).Format(pair.Value, dest);
					sep = " ";
				}
				dest.Write(" >>");
			} else if( val is IEnumerable ) {
				dest.Write("[");
				string sep = "";
				foreach( object item in (IEnumerable)val ) {
					dest.Write(sep);
					((IFormatter)this).Format(item, dest);
					sep = " ";
				}
				dest.Write("]");
			} else {
				FormatString(val.ToString(), dest);
			}
		}
	}
	
	class PrintOp : Op {
		IFormatter formatter;
		string postfix;
		public PrintOp(IFormatter formatter, string postfix="") {
			this.formatter = formatter;
			this.postfix = postfix;
		}
		void Op.Do(Interpreter interp) {
			object value = interp.ThunkToValue<object>(interp.PopThunk());
			this.formatter.Format(value, interp.OutputStream);
			interp.OutputStream.Write(postfix);
		}
	}

	class QuitOp : Op {
		void Op.Do(Interpreter interp) {
			throw new QuitException();
		}
	}

	class OpenProcedureOp : Op {
		void Op.Do(Interpreter interp) {
			throw new Exception(this.GetType()+" can't be executed directly");
		}
	}
	class CloseProcedureOp : Op {
		void Op.Do(Interpreter interp) {
			throw new Exception(this.GetType()+" can't be executed directly");
		}
	}

	static class DictUtil {
		public static void AddAll(DefDict target, DefDict toBeAdded) {
			foreach( var pair in toBeAdded ) {
				target.Add(pair.Key, pair.Value);
			}
		}
	}

	public static class StandardOps {
		public static DefDict Definitions = new DefDict();
		static StandardOps() {
			// Parameterized ops
			Definitions["http://ns.nuke24.net/TScript34/Op/Alias"] = new AliasOpConstructor();
			Definitions["http://ns.nuke24.net/TScript34/Op/Encode"] = new EncodeOpConstructor();
			Definitions["http://ns.nuke24.net/TScript34/Op/Decode"] = new DecodeOpConstructor();
			Definitions["http://ns.nuke24.net/TScript34/Op/PushValue"] = new PushValueOpConstructor();
			// Regular ops
			Definitions["http://ns.nuke24.net/TScript34/Ops/CloseProcedure"] = new CloseProcedureOp();
			Definitions["http://ns.nuke24.net/TScript34/Ops/CountToMark"] = new CountToMarkOp();
			Definitions["http://ns.nuke24.net/TScript34/Ops/ArrayFromStack"] = new ArrayFromStackOp();
			Definitions["http://ns.nuke24.net/TScript34/Ops/DictFromStack"] = new DictFromStackOp();
			Definitions["http://ns.nuke24.net/TScript34/Ops/Define"] = new DefineOp();
			Definitions["http://ns.nuke24.net/TScript34/Ops/Dup"] = new DupOp();
			Definitions["http://ns.nuke24.net/TScript34/Ops/Exch"] = new ExchOp();
			Definitions["http://ns.nuke24.net/TScript34/Ops/Exec"] = new ExecOp();
			Definitions["http://ns.nuke24.net/TScript34/Ops/FetchUri"] = new FetchUriOp();
			Definitions["http://ns.nuke24.net/TScript34/Ops/FlattenStringList"] = new FlattenStringListOp();
			Definitions["http://ns.nuke24.net/TScript34/Ops/ForEach"] = new ForEachOp();
			Definitions["http://ns.nuke24.net/TScript34/Ops/GetElement"] = new GetElementOp();
			Definitions["http://ns.nuke24.net/TScript34/Ops/OpenProcedure"] = new OpenProcedureOp();
			Definitions["http://ns.nuke24.net/TScript34/Ops/Pop"] = new PopOp();
			Definitions["http://ns.nuke24.net/TScript34/Ops/Print"] = new PrintOp(ToStringFormatter.Instance, "");
			Definitions["http://ns.nuke24.net/TScript34/Ops/PrintAsPostScriptSource"] = new PrintOp(PostScriptSourceFormatter.Instance, "\n");
			Definitions["http://ns.nuke24.net/TScript34/Ops/PrintLine"] = new PrintOp(ToStringFormatter.Instance, "\n");
			Definitions["http://ns.nuke24.net/TScript34/Ops/PushMark"] = new PushOp(Mark.Thunk);
			Definitions["http://ns.nuke24.net/TScript34/Ops/Quit"] = new QuitOp();
			// Datatypes
			Definitions["http://www.w3.org/2001/XMLSchema#decimal"] = new DecimalEncoding();
			Definitions["http://ns.nuke24.net/TScript34/Datatypes/Symbol"] = new SymbolEncoding();
		}
	}

	#nullable enable
	public record TS34EncodingList {
		public string EncodingUri; 
		public TS34EncodingList? PreviousEncodings;

		public TS34EncodingList(string uri, TS34EncodingList? previousEncodings) {
			this.EncodingUri = uri;
			this.PreviousEncodings = previousEncodings;
		}
		public TS34EncodingList(string uri) {
			this.EncodingUri = uri;
			this.PreviousEncodings = null;
		}

		public override string ToString() {
			return this.EncodingUri + (this.PreviousEncodings == null ? "" : " " + this.PreviousEncodings);
		}

		public static TS34EncodingList Uri = new TS34EncodingList("http://ns.nuke24.net/TOGVM/Datatypes/URIResource", null);
		public static TS34EncodingList ThunkedValueCollection = new TS34EncodingList("http://ns.nuke24.net/TScript34/Datatypes/ThunkedValueCollection", null);
	}
	public record TS34Thunk {
		public object EncodedValue;
		public TS34EncodingList? Encodings;

		public TS34Thunk(object value, TS34EncodingList? encodings) {
			this.EncodedValue = value;
			this.Encodings = encodings;
		}
		public TS34Thunk(object value) {
			this.EncodedValue = value;
			this.Encodings = null;
		}
		
		public override string ToString() {
			return this.EncodedValue + (this.Encodings == null ? "" : " " + this.Encodings);
		}
		
		public static TS34Thunk ForUri(string uri) {
			return new TS34Thunk(uri, TS34EncodingList.Uri);
		}
	}
	#nullable disable

	static class ValueUtil {
		public static string Describe(object o) {
			// TODO: always include type, but for simple values (numbers, short strings, thunks, etc)
			// also include the data
			return DescribeValueOfType(o.GetType());
		}
		public static string DescribeValueOfType(System.Type type) {
			return "a "+type;
		}
		public static T LosslesslyConvert<T>(object value) {
			if( value is T ) return (T)value;

			if( value is System.IConvertible ) {
				T converted = (T)System.Convert.ChangeType(value, System.Type.GetTypeCode(typeof(T)));
				object convertedBack = System.Convert.ChangeType(converted, ((System.IConvertible)value).GetTypeCode() );
				if( value.Equals(convertedBack) ) {
					return converted;
				} else {
					throw new Exception( $"{Describe(value)} cannot be losslessly converted to {DescribeValueOfType(typeof(T))}" );
				}
			}

			throw new Exception($"Don't know how to convert {Describe(value)} to {DescribeValueOfType(typeof(T))}");
		}
	}
	
	public class Interpreter : IUriResolver {
		static char[] whitespace = new char[] { ' ', '\t', '\r' };
		
		public DefDict Definitions = new DefDict();
		public SCG.List<TS34Thunk> DataStack = new SCG.List<TS34Thunk>();
		public IUriResolver UriResolver = new AUriResolver();
		
		protected class InterpreterThunkCodec : ICodec<object,TS34Thunk> {
			Interpreter Interp;
			public InterpreterThunkCodec(Interpreter interp) {
				this.Interp = interp;
			}
			public TS34Thunk Encode(object obj) {
				return Interp.ValueToThunk(obj);
			}
			public object Decode(TS34Thunk thunk) {
				return Interp.ThunkToValue<object>(thunk);
			}
		}

		protected InterpreterThunkCodec ThunkCodec;
		
		public Interpreter() {
			this.ThunkCodec = new InterpreterThunkCodec(this);
			// Pretty basic and necessary for functioning!
			this.Definitions["http://ns.nuke24.net/TOGVM/Datatypes/URIResource"] = new UriReferenceEncoding(this);
			this.Definitions["http://ns.nuke24.net/TScript34/Datatypes/ThunkedValueCollection"] = new ThunkedValueCollectionEncoding(this.ThunkCodec);
		}

		public object Resolve(string uri) {
			if( this.Definitions.ContainsKey(uri) ) {
				return this.Definitions[uri];
			} else {
				return this.UriResolver.Resolve(uri);
			}
		}
		
		public ISimpleOutput OutputStream = new ErroringOutput();
		
		public void DefineAll(DefDict defs) {
			DictUtil.AddAll(this.Definitions, defs);
		}

		public TS34Thunk Peek() {
			if( DataStack.Count == 0 ) throw new Exception("Stack underflow!");
			return DataStack[DataStack.Count - 1];
		}

		public TS34Thunk PopThunk() {
			// System.Console.WriteLine("Popping from stack, which has "+DataStack.Count+" items...");
			var index = DataStack.Count-1;
			TS34Thunk value = DataStack[index];
			DataStack.RemoveAt(index);
			return value;
		}
		public void PushThunk(TS34Thunk value) {
			DataStack.Add(value);
			//System.Console.WriteLine("Pushing "+value+"; stack now has "+this.DataStack.Count+" items");
		}
		public void PushValue(object value) {
			this.PushThunk(this.ValueToThunk(value));
		}
		public object PopValue() {
			return this.ThunkToValue<object>(this.PopThunk());
		}

		public int CountToMark() {
			var count = 0;
			for( var index = DataStack.Count - 1; index >= 0; --index, ++count ) {
				if( DataStack[index] == Mark.Thunk ) {
					return count;
				}
			}
			throw new Exception("No mark on stack!");			
		}

		public TS34Thunk ValueToThunk(object val) {
			return new TS34Thunk(val); // No encodings => EncodedValue is the entity
		}
		public T ThunkToValue<T>(TS34Thunk thunk, System.Type t) {
			object val = thunk.EncodedValue;
			for( TS34EncodingList el = thunk.Encodings; el != null; el = el.PreviousEncodings ) {
				object encodingVal = ((IUriResolver)this).Resolve(el.EncodingUri);
				if( !(encodingVal is IEncoding) ) {
					throw new Exception("Uh oh; "+el.EncodingUri+" does not name an encoding; got a "+encodingVal.GetType());
				}
				val = ((IEncoding)encodingVal).Decode(val);
			}
			return ValueUtil.LosslesslyConvert<T>(val);
		}
		public T ThunkToValue<T>(TS34Thunk thunk) {
			return this.ThunkToValue<T>(thunk, typeof(T));
		}
		public T ThunkToValueShallow<T>(TS34Thunk thunk) {
			if( thunk.Encodings == TS34EncodingList.ThunkedValueCollection ) {
				return (T)thunk.EncodedValue;
			} else {
				return ThunkToValue<T>(thunk);
			}
		}
		public TS34Thunk ThunkedValueCollectionToThunk(SCG.IDictionary<object,TS34Thunk> collection) {
			return new TS34Thunk(collection, TS34EncodingList.ThunkedValueCollection);
		}
		public TS34Thunk ThunkedValueCollectionToThunk(SCG.IList<TS34Thunk> collection) {
			return new TS34Thunk(collection, TS34EncodingList.ThunkedValueCollection);
		}
		
		SCG.List<SCG.List<Op>> procedureDefinitionStack = new SCG.List<SCG.List<Op>>();

		public void DoCommand(IStringList args) {
			if( args.Count == 0 ) throw new ArgumentException("DoCommand requires at least one thing in the args array");
			
			object def = ((IUriResolver)this).Resolve(args[0]);
			if( def == null ) {
				throw new ArgumentException("'"+args[0]+"' not defined");
			}
			if( def is OpConstructor ) {
				// WHY IS THERE NO SLICE IN DOTNET
				StringList opArgs = new StringList();
				for( int i=1; i<args.Count; ++i ) opArgs.Add(args[i]);
				def = ((OpConstructor)def).Parse(opArgs, this);
			}
			// TODO: Some ops can be compile time ops
			if( def is OpenProcedureOp ) {
				procedureDefinitionStack.Add(new SCG.List<Op>());
				return;
			} else if( def is CloseProcedureOp ) {
				if( procedureDefinitionStack.Count == 0 ) {
					throw new Exception("Can't close procedure; not currently defining one!");
				}
				def = new PushOp(this.ValueToThunk(new Procedure(procedureDefinitionStack[procedureDefinitionStack.Count-1])));
				procedureDefinitionStack.RemoveAt(procedureDefinitionStack.Count-1);
			}
			if( def is Op ) {
				if( procedureDefinitionStack.Count == 0 ) {
					((Op)def).Do(this);
				} else {
					procedureDefinitionStack[procedureDefinitionStack.Count-1].Add((Op)def);
				}
			} else {
				throw new Exception(args[0]+" does not name an op, but a "+def.GetType());
			}
		}
		
		public void HandleLine(string line) {
			line = line.Trim();
			if( line.Length == 0 ) return;
			if( line[0] == '#' ) return;
			IStringList parts = line.Split(whitespace, StringSplitOptions.RemoveEmptyEntries);
			DoCommand(parts);
		}

		protected delegate void ReaderHandler(TextReader s);
		protected void StreamFile(string filename, ReaderHandler h) {
			if( filename == "-" ) {
				h( Console.In );
			} else if( filename.Contains(":") ) {
				h( new System.IO.StringReader(((IUriResolver)this).Resolve(filename).ToString()) );
			} else {
				StreamReader sr = new StreamReader(filename);
				h(sr);
				sr.Close();
				// Assume file
			}
		}
		
		public void DoMain(string[] args) {
			StringList scriptFiles = new StringList();
			StringList scriptArgs = new StringList();
			
			bool mainScriptIndicated = false;
			for( int i=0; i<args.Length; ++i ) {
				if( mainScriptIndicated ) {
					scriptArgs.Add(args[i]);
				} else if( args[i] == "-f" ) {
					if( args.Length <= i+1 ) {
						throw new Exception("-f requires an additional [script file] argument");
					}
					scriptFiles.Add(args[i+1]);
					++i;
				} else if( !args[i].StartsWith("-") ) {
					scriptFiles.Add(args[i]);
					mainScriptIndicated = true;
				} else {
					throw new Exception("Unrecognized argument: "+args[i]);
				}
			}
			
			if( scriptFiles.Count == 0 ) {
				scriptFiles.Add("-");
			}
			
			string line;
			string sourceFilename = "(not yet initialized)";
			int sourceLineNumber = 0;
			bool okay = false;
			try {
				foreach( string path in scriptFiles ) {
					sourceFilename = path;
					sourceLineNumber = 1;
					this.StreamFile(path, delegate(TextReader r) {
						while( (line = r.ReadLine()) != null ) {
							this.HandleLine(line);
							++sourceLineNumber;
						}
					});
				}
				okay = true;
			} catch( QuitException ) {
			} finally {
				if( !okay ) {
					System.Console.Error.Write($"Error encountered at {sourceFilename}:{sourceLineNumber}");
				}
			}
		}

		public static void Main(string[] args) {
			NoCheckCertificatePolicy.Init();
			Interpreter interp = new Interpreter();
			
			interp.OutputStream = new StreamOutput(System.Console.OpenStandardOutput());
			interp.DefineAll(StandardOps.Definitions);
			interp.DoMain(args);
		}
	}
}
