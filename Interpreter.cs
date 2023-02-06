// -*- insert-tabs-mode: t; tab-width: 3; c-basic-offset: 3 -*-

using ArgumentException = System.ArgumentException;
using Console = System.Console;
using DefDict = System.Collections.Generic.Dictionary<string, object>;
using Exception = System.Exception;
using HttpClient = System.Net.Http.HttpClient;
using Int32 = System.Int32;
using IStringList = System.Collections.Generic.IList<string>;
using Object = System.Object;
using Stack = System.Collections.Generic.List<object>;
using StringBuilder = System.Text.StringBuilder;
using StringList = System.Collections.Generic.List<string>;
using StringSplitOptions = System.StringSplitOptions;
using Uri = System.Uri;

using System.Collections.Generic;

using TOGoS.TScrpt34_2.MapStuff;

namespace TOGoS.TScrpt34_2 {
	interface Op {
		void Do(Interpreter interp);
	}
	interface OpConstructor {
		Op Parse(IStringList args);
	}

	class Mark {}

	interface IUriResolver {
		object Resolve(string uri);
	}

	class AUriResolver : IUriResolver {
		object IUriResolver.Resolve(string uri) {
			using (HttpClient client = new HttpClient()) {
				// For now just strings
				string s = client.GetStringAsync(uri).Result;
				return s;
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
			object value = interp.definitions[this.targetName];
			if( value == null ) {
				throw new Exception("alias: '"+this.targetName+"' not defined");
			}
			interp.definitions[this.name] = value;
		}
	}
	class AliasOpConstructor : OpConstructor {
		Op OpConstructor.Parse(IStringList args) {
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
	class CreateArrayOp : Op {
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
	class DupOp : Op {
		void Op.Do(Interpreter interp) {
			if( interp.DataStack.Count < 1 ) throw new Exception("Can't dup; stack empty!");
			interp.Push(interp.Peek());
		}
	}
	class FetchUriOp : Op {
		void Op.Do(Interpreter interp) {
			string uri = (string)interp.Pop();
			interp.Push(new UriResource(uri, interp.UriResolver));
		}
	}
	/** i.e. take all the strings in an array and concatenate them together */
	class FlattenStringListOp : Op {
		void Op.Do(Interpreter interp) {
			IEnumerable<object> list = (IEnumerable<object>)interp.Pop();
			StringBuilder sb = new StringBuilder();
			foreach( var item in list ) sb.Append(item.ToString());
			interp.Push(sb.ToString());
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
	class PushStringOpConstructor : OpConstructor {
		string resolve(string uri) {
			if( uri.StartsWith("data:,") ) {
				return Uri.UnescapeDataString(uri.Substring(6));
			} else {
				throw new ArgumentException("string argument not a data:,... URI: '"+uri+"'");
			}
		}
		
		Op OpConstructor.Parse(IStringList args) {
			if( args.Count != 1 ) {
				throw new ArgumentException("'push-string' requires exactly one argument");
			}
			return new PushOp(resolve(args[0]));
		}
	}
	// TODO: Generic push value with datatype argument!
	class PushInt32OpConstructor : OpConstructor {
		int resolve(string uri) {
			if( uri.StartsWith("data:,") ) {
				return Int32.Parse(Uri.UnescapeDataString(uri.Substring(6)));
			} else {
				throw new ArgumentException("string argument not a data:,... URI: '"+uri+"'");
			}
		}
		
		Op OpConstructor.Parse(IStringList args) {
			if( args.Count != 1 ) {
				throw new ArgumentException("'push-string' requires exactly one argument");
			}
			return new PushOp(resolve(args[0]));
		}
	}
	class PrintOp : Op {
		string postfix;
		public PrintOp(string postfix="") {
			this.postfix = postfix;
		}
		void Op.Do(Interpreter interp) {
			var value = interp.Pop();
			if( value is IList<PointInfo<LatLongPosition,VegData>> ) {
				System.Console.Write("[");
				foreach( var item in (IList<PointInfo<LatLongPosition,VegData>>)value ) {
					System.Console.Write(" ");
					System.Console.Write(item);
				}
				System.Console.Write("]");
			} else {
				System.Console.Write(value);
			}
			System.Console.Write(postfix);
		}
	}

	#region Map Ops
	// These should not be in Interpreter.cs

	/**
	 * Pops JSON (or something that ToString()s to JSON
	 * And returns a pointinfo list of lat/long to vegdata
	*/
	class DecodeVegLatLongOp : Op {
		void Op.Do(Interpreter interp) {
			string json = interp.Pop().ToString();
			var pointList = new MapStuff.Decoder<MapStuff.LatLongPosition, MapStuff.VegData>().Decode(json);
			interp.Push(pointList);
		}
	}
	#endregion

	class Interpreter {
		static char[] whitespace = new char[] { ' ', '\t', '\r' };

		public DefDict definitions = new DefDict();
		public Stack DataStack = new Stack();
		public readonly AUriResolver UriResolver = new AUriResolver();

		public Interpreter() {
			definitions["alias"] = new AliasOpConstructor();
			definitions["http://ns.nuke24.net/TScript34/Op/PushString"] = new PushStringOpConstructor();
			definitions["http://ns.nuke24.net/TScript34/Op/PushInt32"] = new PushInt32OpConstructor();
			definitions["http://ns.nuke24.net/TScript34/Ops/CountToMark"] = new CountToMarkOp();
			definitions["http://ns.nuke24.net/TScript34/Ops/CreateArray"] = new CreateArrayOp();
			definitions["http://ns.nuke24.net/TScript34/Ops/Dup"] = new DupOp();
			definitions["http://ns.nuke24.net/TScript34/Ops/FetchUri"] = new FetchUriOp();
			definitions["http://ns.nuke24.net/TScript34/Ops/FlattenStringListOp"] = new FlattenStringListOp();
			definitions["http://ns.nuke24.net/TScript34/Ops/Print"] = new PrintOp("");
			definitions["http://ns.nuke24.net/TScript34/Ops/PrintLine"] = new PrintOp("\n");
			definitions["http://ns.nuke24.net/TScript34/Ops/PushMark"] = new PushOp(new Mark());

			definitions["http://ns.nuke24.net/TScript34/MapStuff/Ops/DecodeVegLatLong"] = new DecodeVegLatLongOp();
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
		
		public void DoCommand(IStringList args) {
			if( args.Count == 0 ) throw new ArgumentException("DoCommand requires at least one thing in the args array");

			object def = definitions[args[0]];
			if( def == null ) {
				throw new ArgumentException("'"+args[0]+"' not defined");
			}
			if( def is OpConstructor ) {
				// WHY IS THERE NO SLICE IN DOTNET
				StringList opArgs = new StringList();
				for( int i=1; i<args.Count; ++i ) opArgs.Add(args[i]);
				def = ((OpConstructor)def).Parse(opArgs);
			}
			if( def is Op ) {
				((Op)def).Do(this);
			}
		}
		
		public void HandleLine(string line) {
			line = line.Trim();
			if( line.Length == 0 ) return;
			if( line[0] == '#' ) return;
			IStringList parts = line.Split(whitespace, StringSplitOptions.RemoveEmptyEntries);
			DoCommand(parts);
		}
		


		public static void Main(string[] args) {
			NoCheckCertificatePolicy.Init();

			string line;
			Interpreter interp = new Interpreter();
			while( (line = Console.ReadLine()) != null ) {
				interp.HandleLine(line);
			}
		}
	}
}
