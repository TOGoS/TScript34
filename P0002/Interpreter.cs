// -*- insert-tabs-mode: t; tab-width: 3; c-basic-offset: 3 -*-

using ArgumentException = System.ArgumentException;
using Console = System.Console;
using DefDict = System.Collections.Generic.Dictionary<string, object>;
using Exception = System.Exception;
using Float64 = System.Double;
using HttpClient = System.Net.Http.HttpClient;
using IEnumerable = System.Collections.IEnumerable;
using Int32 = System.Int32;
using IList = System.Collections.IList;
using IStringList = System.Collections.Generic.IList<string>;
using Object = System.Object;
using Stack = System.Collections.Generic.List<object>;
using StringBuilder = System.Text.StringBuilder;
using StringList = System.Collections.Generic.List<string>;
using StringSplitOptions = System.StringSplitOptions;
using Uri = System.Uri;

using SCG = System.Collections.Generic;

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
	
	class QuitException : Exception {}
	
	class Mark {}
	
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
			object value = interp.definitions[this.targetName];
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
		object Decode(object lexical, string datatype) {
			if( datatype == "http://www.w3.org/2001/XMLSchema#decimal" ) {
				return Float64.Parse(lexical.ToString());
			} else {
				throw new Exception("Unknown datatype: "+datatype);
			}
		}

		Op OpConstructor.Parse(IStringList args, IUriResolver resolver) {
			if( args.Count < 1 ) {
				throw new ArgumentException("'push-value' expects one or more arguments (value, encoding1, encoding2...)");
			}
			object value = resolver.Resolve(args[0]);
			for( int i=1; i<args.Count; ++i ) {
				// Apply lexical-to-value mapping of each encoding ('datatype')
				value = this.Decode(value, args[i]);
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

	class PrintOp : Op {
		string postfix;
		public PrintOp(string postfix="") {
			this.postfix = postfix;
		}
		void Op.Do(Interpreter interp) {
			var value = interp.Pop();
			if( value is string ) {
				System.Console.Write(value);
			} else if( value is IEnumerable ) {
				System.Console.Write("[");
				foreach( var item in (IEnumerable)value ) {
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
	 * json -- List<PointInfo<?>>
	*/
	class DecodePointListOp : Op {
		void Op.Do(Interpreter interp) {
			string json = interp.Pop().ToString();
			var pointList = new MapStuff.JsonDecoder<MapStuff.LatLongPosition, MapStuff.VegData>().Decode(json);
			interp.Push(pointList);
		}
	}

	class QuitOp : Op {
		void Op.Do(Interpreter interp) {
			throw new QuitException();
		}
	}
	
	/**
	 * List<PointInfo<LatLongPosition,?>> centerlat centerlong diameter -- List<PointInfo<XYPosition,?>>
	 * lat/long are in degrees
	 */
	class LatLongToXYPointListOp : Op {
		void Op.Do(Interpreter interp) {
			double diameter = (double)interp.Pop();
			double centerLong = (double)interp.Pop();
			double centerLat = (double)interp.Pop();
			IEnumerable inputList = (IEnumerable)interp.Pop();
			// TODO: Should be Dat-agnostic!  How to do that?
			SCG.List<PointInfo<XYPosition,VegData>> outputList = new SCG.List<PointInfo<XYPosition,VegData>>();
			PointInfoConverter<LatLongPosition,XYPosition,VegData> plc =
				new PointInfoConverter<LatLongPosition,XYPosition,VegData>(
					new LatLongToXYConverter(diameter, new LatLongPosition(centerLong,centerLat)).LatLongToXY
				);
			foreach( object item in inputList ) {
				PointInfo<LatLongPosition,VegData> point = (PointInfo<LatLongPosition,VegData>)item;
				outputList.Add(plc.ConvertPointPosition(point));
			}
			interp.Push(outputList);
		}
	}
	#endregion

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

		public Interpreter() {
			definitions["alias"] = new AliasOpConstructor();
			definitions["http://ns.nuke24.net/TScript34/Op/PushValue"] = new PushValueOpConstructor();
			definitions["http://ns.nuke24.net/TScript34/Op/PushString"] = new PushStringOpConstructor();
			definitions["http://ns.nuke24.net/TScript34/Op/PushInt32"] = new PushInt32OpConstructor();
			definitions["http://ns.nuke24.net/TScript34/Op/PushFloat64"] = new PushFloat64OpConstructor();
			definitions["http://ns.nuke24.net/TScript34/Ops/CountToMark"] = new CountToMarkOp();
			definitions["http://ns.nuke24.net/TScript34/Ops/ArrayFromStack"] = new ArrayFromStackOp();
			definitions["http://ns.nuke24.net/TScript34/Ops/DictFromStack"] = new DictFromStackOp();
			definitions["http://ns.nuke24.net/TScript34/Ops/Dup"] = new DupOp();
			definitions["http://ns.nuke24.net/TScript34/Ops/FetchUri"] = new FetchUriOp();
			definitions["http://ns.nuke24.net/TScript34/Ops/FlattenStringListOp"] = new FlattenStringListOp();
			definitions["http://ns.nuke24.net/TScript34/Ops/GetElement"] = new GetElementOp();
			definitions["http://ns.nuke24.net/TScript34/Ops/Print"] = new PrintOp("");
			definitions["http://ns.nuke24.net/TScript34/Ops/PrintLine"] = new PrintOp("\n");
			definitions["http://ns.nuke24.net/TScript34/Ops/PushMark"] = new PushOp(new Mark());
			definitions["http://ns.nuke24.net/TScript34/Ops/Quit"] = new QuitOp();
			
			// Map stuff
			definitions["http://ns.nuke24.net/TScript34/MapStuff/Ops/DecodePointList"] = new DecodePointListOp();
			definitions["http://ns.nuke24.net/TScript34/MapStuff/Ops/LatLongToXYPointList"] = new LatLongToXYPointListOp();
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
				def = ((OpConstructor)def).Parse(opArgs, this);
			}
			if( def is Op ) {
				((Op)def).Do(this);
			}
		}
		
		public void HandleLine(string line, int lineNumber) {
			line = line.Trim();
			if( line.Length == 0 ) return;
			if( line[0] == '#' ) return;
			IStringList parts = line.Split(whitespace, StringSplitOptions.RemoveEmptyEntries);
			DoCommand(parts);
		}
		
		public static void Main(string[] args) {
			NoCheckCertificatePolicy.Init();

			int lineNumber = 1;
			string line;
			Interpreter interp = new Interpreter();
			try {
				while( (line = Console.ReadLine()) != null ) {
					interp.HandleLine(line, lineNumber);
					++lineNumber;
				}
			} catch( QuitException e ) {
			}
		}
	}
}
