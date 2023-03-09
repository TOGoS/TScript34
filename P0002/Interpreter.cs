// -*- insert-tabs-mode: t; tab-width: 3; c-basic-offset: 3 -*-

using ArgumentException = System.ArgumentException;
using Console = System.Console;
using DefDict = System.Collections.Generic.Dictionary<string, object>;
using Exception = System.Exception;
using Float64 = System.Double;
using HttpClient = System.Net.Http.HttpClient;
using IDictionary = System.Collections.IDictionary;
using IEnumerable = System.Collections.IEnumerable;
using IList = System.Collections.IList;
using Int32 = System.Int32;
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

using TOGoS.TScrpt34_2.MapStuff;

namespace TOGoS.TScrpt34_2 {
	interface Op {
		void Do(Interpreter interp);
	}
	interface OpConstructor {
		Op Parse(IStringList args, IUriResolver resolver);
	}
	class Procedure : Op {
		SCG.IList<Op> Operations;
		public Procedure(SCG.IList<Op> ops) {
			this.Operations = ops;
		}
		void Op.Do(Interpreter interp) {
			foreach(Op op in this.Operations) {
				// Maybe some way to return idk
				op.Do(interp);
			}
		}
	}
	
	interface IEncoding {
		object Encode(object v);
		object Decode(object l);
	}

	class DecimalEncoding : IEncoding {
		// TODO: Make sure this is actually following http://www.w3.org/2001/XMLSchema#decimal
		object IEncoding.Encode(object v) {
			return ((double)v).ToString();
		}
		object IEncoding.Decode(object l) {
			return Float64.Parse(l.ToString());
		}
	}
	class SymbolEncoding : IEncoding {
		// Treating symbols as strings for now
		object IEncoding.Encode(object v) {
			return v.ToString();
		}
		object IEncoding.Decode(object l) {
			return l.ToString();
		}
	}

	class QuitException : Exception {}
	
	class Mark {
		private Mark() { }
		public static Mark Instance = new Mark();
	}
	
	interface IUriResolver {
		object Resolve(string uri);
	}

	class AUriResolver : IUriResolver {
		object IUriResolver.Resolve(string uri) {
			if( uri.StartsWith("data:,") ) {
				return Uri.UnescapeDataString(uri.Substring(6));
			} else {
				using (HttpClient client = new HttpClient()) {
					// For now just strings
					string s = client.GetStringAsync(uri).Result;
					return s;
				}
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
			interp.definitions[this.name] = value;
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
			interp.Push(interp.CountToMark());
		}
	}

	/** (item1 item2 ... itemN n ArrayFromStack -- array) */
	class ArrayFromStackOp : Op {
		void Op.Do(Interpreter interp) {
			int length = (int)interp.Pop();
			int stackOffset = interp.DataStack.Count - length;
			if( stackOffset < 0 ) {
				throw new Exception("Can't create array of "+length+" elements; only "+interp.DataStack.Count+" on stack!");
			}
			Stack arr = new Stack();
			arr.AddRange(interp.DataStack.GetRange(stackOffset, length));
			interp.DataStack.RemoveRange(stackOffset, length);
			interp.Push(arr);
		}
	}
	class DefineOp : Op {
		void Op.Do(Interpreter interp) {
			object value = interp.Pop();
			object key = interp.Pop();
			interp.definitions.Add(key.ToString(), value);
		}
	}
	class DictFromStackOp : Op {
		void Op.Do(Interpreter interp) {
			int length = (int)interp.Pop();
			if( (length & 1) != 0 ) {
				throw new Exception("DictFromStackOp requires an even number of stack elements; "+length+" were indicated");
			}
			int stackOffset = interp.DataStack.Count - length;
			if( stackOffset < 0 ) {
				throw new Exception("Can't create array of "+length+" elements; only "+interp.DataStack.Count+" on stack!");
			}
			System.Collections.Generic.Dictionary<object,object> dict = new System.Collections.Generic.Dictionary<object,object>();
			for( int i=stackOffset; i<interp.DataStack.Count; i += 2 ) {
				// TODO: Translate strings to name objects as per postscript spec
				dict[interp.DataStack[i]] = interp.DataStack[i+1];
			}
			interp.DataStack.RemoveRange(stackOffset, length);
			interp.Push(dict);
		}
	}
	class DupOp : Op {
		void Op.Do(Interpreter interp) {
			if( interp.DataStack.Count < 1 ) throw new Exception("Can't dup; stack empty!");
			interp.Push(interp.Peek());
		}
	}
	class ExchOp : Op {
		void Op.Do(Interpreter interp) {
			object a = interp.Pop();
			object b = interp.Pop();
			interp.Push(a);
			interp.Push(b);
		}
	}
	class ExecOp : Op {
		void Op.Do(Interpreter interp) {
			((Op)interp.Pop()).Do(interp);
		}
	}
	class FetchUriOp : Op {
		void Op.Do(Interpreter interp) {
			string uri = (string)interp.Pop();
			interp.Push(new UriResource(uri, interp));
		}
	}
	/** i.e. take all the strings in an array and concatenate them together */
	class FlattenStringListOp : Op {
		void Op.Do(Interpreter interp) {
			IEnumerable list = (IEnumerable)interp.Pop();
			StringBuilder sb = new StringBuilder();
			foreach( var item in list ) sb.Append(item.ToString());
			interp.Push(sb.ToString());
		}
	}

	/** Equivalent to postscript's forall (list proc -- ...) */
	class ForEachOp : Op {
		void Op.Do(Interpreter interp) {
			Op proc = (Op)interp.Pop();
			IEnumerable list = (IEnumerable)interp.Pop();
			foreach( object item in list ) {
				interp.Push(item);
				proc.Do(interp);
			}
		}
	}

	/**
	 * Equivalent to postscripts 'get' (collection key -- value)
	*/
	class GetElementOp : Op {
		void Op.Do(Interpreter interp) {
			object key = interp.Pop();
			object collection = interp.Pop();
			if( collection is SCG.IDictionary<object,object> ) {
				interp.Push( ((SCG.IDictionary<object,object>)collection)[key]);
			} else if( collection is IList ) {
				interp.Push( ((IList)collection)[(int)key] );
			} else {
				throw new Exception("Don't know how to get element of "+collection.GetType());
			}
		}
	}
	class PopOp : Op {
		void Op.Do(Interpreter interp) {
			interp.Pop();
		}
	}
	class PushOp : Op {
		object toPush;
		public PushOp(object toPush) {
			this.toPush = toPush;
		}
		void Op.Do(Interpreter interp) {
			interp.Push(toPush);
		}
	}
	
	class PushValueOpConstructor : OpConstructor {
		Op OpConstructor.Parse(IStringList args, IUriResolver resolver) {
			if( args.Count < 1 ) {
				throw new ArgumentException("'push-value' expects one or more arguments (value, encoding1, encoding2...)");
			}
			object value = resolver.Resolve(args[0]);
			for( int i=1; i<args.Count; ++i ) {
				IEncoding e = (IEncoding)resolver.Resolve(args[i]);
				//Console.WriteLine("# Decoding "+value)
				value = e.Decode(value);
			}
			return new PushOp(value);
		}
	}
	class PushStringOpConstructor : OpConstructor {		
		Op OpConstructor.Parse(IStringList args, IUriResolver resolver) {
			if( args.Count != 1 ) {
				throw new ArgumentException("'push-string' requires exactly one argument");
			}
			return new PushOp(resolver.Resolve(args[0]).ToString());
		}
	}
	class PushFloat64OpConstructor : OpConstructor {
		Op OpConstructor.Parse(IStringList args, IUriResolver resolver) {
			if( args.Count != 1 ) {
				throw new ArgumentException("'push-string' requires exactly one argument");
			}
			return new PushOp(Float64.Parse(resolver.Resolve(args[0]).ToString()));
		}
	}
	class PushInt32OpConstructor : OpConstructor {
		Op OpConstructor.Parse(IStringList args, IUriResolver resolver) {
			if( args.Count != 1 ) {
				throw new ArgumentException("'push-string' requires exactly one argument");
			}
			return new PushOp(Int32.Parse(resolver.Resolve(args[0]).ToString()));
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
			dest.Write(val.ToString());
		}		
	}
	class PostScriptSourceFormatter : IFormatter {
		public static PostScriptSourceFormatter Instance = new PostScriptSourceFormatter();
		
		void IFormatter.Format(object val, ISimpleOutput dest) {
			if( val is Int32 || val is Float64 ) {
				dest.Write(val.ToString());
			} else if( val is IDictionary ) {
				string sep = " ";
				dest.Write("<<");
				foreach( System.Collections.DictionaryEntry pair in (IDictionary)val ) {
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
				dest.Write("("+val.ToString().Replace("\\","\\\\").Replace("(","\\(").Replace(")","\\)")+")");
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
			this.formatter.Format(interp.Pop(), interp.OutputStream);
			System.Console.Write(postfix);
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

	static class StandardOps {
		public static DefDict Definitions = new DefDict();
		static StandardOps() {
			// Parameterized ops
			Definitions["http://ns.nuke24.net/TScript34/Op/Alias"] = new AliasOpConstructor();
			Definitions["http://ns.nuke24.net/TScript34/Op/PushValue"] = new PushValueOpConstructor();
			Definitions["http://ns.nuke24.net/TScript34/Op/PushString"] = new PushStringOpConstructor();
			Definitions["http://ns.nuke24.net/TScript34/Op/PushInt32"] = new PushInt32OpConstructor();
			Definitions["http://ns.nuke24.net/TScript34/Op/PushFloat64"] = new PushFloat64OpConstructor();
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
			Definitions["http://ns.nuke24.net/TScript34/Ops/FlattenStringListOp"] = new FlattenStringListOp();
			Definitions["http://ns.nuke24.net/TScript34/Ops/ForEach"] = new ForEachOp();
			Definitions["http://ns.nuke24.net/TScript34/Ops/GetElement"] = new GetElementOp();
			Definitions["http://ns.nuke24.net/TScript34/Ops/OpenProcedure"] = new OpenProcedureOp();
			Definitions["http://ns.nuke24.net/TScript34/Ops/Pop"] = new PopOp();
			Definitions["http://ns.nuke24.net/TScript34/Ops/Print"] = new PrintOp(ToStringFormatter.Instance, "");
			Definitions["http://ns.nuke24.net/TScript34/Ops/PrintAsPostScriptSource"] = new PrintOp(PostScriptSourceFormatter.Instance, "\n");
			Definitions["http://ns.nuke24.net/TScript34/Ops/PrintLine"] = new PrintOp(ToStringFormatter.Instance, "\n");
			Definitions["http://ns.nuke24.net/TScript34/Ops/PushMark"] = new PushOp(Mark.Instance);
			Definitions["http://ns.nuke24.net/TScript34/Ops/Quit"] = new QuitOp();
			// Datatypes
			Definitions["http://www.w3.org/2001/XMLSchema#decimal"] = new DecimalEncoding();
			Definitions["http://ns.nuke24.net/TScript34/Datatypes/Symbol"] = new SymbolEncoding();
		}
	}
	
	class Interpreter : IUriResolver {
		static char[] whitespace = new char[] { ' ', '\t', '\r' };

		public DefDict definitions = new DefDict();
		public Stack DataStack = new Stack();
		protected readonly AUriResolver UriResolver = new AUriResolver();

		object IUriResolver.Resolve(string uri) {
			if( this.definitions.ContainsKey(uri) ) {
				return this.definitions[uri];
			} else {
				return ((IUriResolver)this.UriResolver).Resolve(uri);
			}
		}

		class ConsoleOutput : ISimpleOutput {
			public void Write(byte[] data) {
				Console.Out.Write(data);
			}
			public void Write(char[] data) {
				Console.Out.Write(data);
			}
		}


		public ISimpleOutput OutputStream = new ConsoleOutput();
		
		public void DefineAll(DefDict defs) {
			DictUtil.AddAll(this.definitions, defs);
		}

		public object Peek() {
			if( DataStack.Count == 0 ) return null;
			return DataStack[DataStack.Count - 1];
		}

		public object Pop() {
			var index = DataStack.Count-1;
			object value = DataStack[index];
			DataStack.RemoveAt(index);
			return value;
		}
		public void Push(object value) {
			DataStack.Add(value);
		}
		public int CountToMark() {
			var count = 0;
			for( var index = DataStack.Count - 1; index >= 0; --index, ++count ) {
				if( DataStack[index] is Mark ) {
					return count;
				}
			}
			throw new Exception("No mark on stack!");			
		}
		
		SCG.List<SCG.List<Op>> procedureDefinitionStack = new SCG.List<SCG.List<Op>>();

		public void DoCommand(IStringList args) {
			if( args.Count == 0 ) throw new ArgumentException("DoCommand requires at least one thing in the args array");
			
			if( !definitions.ContainsKey(args[0]) ) {
				throw new ArgumentException("'"+args[0]+"' not defined");
			}
			object def = definitions[args[0]];
			if( def is OpConstructor ) {
				// WHY IS THERE NO SLICE IN DOTNET
				StringList opArgs = new StringList();
				for( int i=1; i<args.Count; ++i ) opArgs.Add(args[i]);
				def = ((OpConstructor)def).Parse(opArgs, this);
			}
			if( def is OpenProcedureOp ) {
				procedureDefinitionStack.Add(new SCG.List<Op>());
				return;
			} else if( def is CloseProcedureOp ) {
				if( procedureDefinitionStack.Count == 0 ) {
					throw new Exception("Can't close procedure; not currently defining one!");
				}
				def = new PushOp(new Procedure(procedureDefinitionStack[procedureDefinitionStack.Count-1]));
				procedureDefinitionStack.RemoveAt(procedureDefinitionStack.Count-1);
			}
			if( def is Op ) {
				if( procedureDefinitionStack.Count == 0 ) {
					((Op)def).Do(this);
				} else {
					procedureDefinitionStack[procedureDefinitionStack.Count-1].Add((Op)def);
				}
			}
		}
		
		public void HandleLine(string line, int lineNumber) {
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
		
		public static void Main(string[] args) {
			NoCheckCertificatePolicy.Init();
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
			
			int lineNumber = 1;
			string line;
			Interpreter interp = new Interpreter();
			interp.DefineAll(StandardOps.Definitions);
			interp.DefineAll(MapStuff.MapOps.Definitions);
			try {
				foreach( string path in scriptFiles ) {
					interp.StreamFile(path, delegate(TextReader r) {
						while( (line = r.ReadLine()) != null ) {
							interp.HandleLine(line, lineNumber);
							++lineNumber;
						}
					});
				}
			} catch( QuitException ) {
			}
		}
	}
}
