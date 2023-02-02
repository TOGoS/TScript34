// -*- insert-tabs-mode: t; tab-width: 3; c-basic-offset: 3 -*-

using ArgumentException = System.ArgumentException;
using Console = System.Console;
using StringSplitOptions = System.StringSplitOptions;

namespace TOGoS.TScrpt34_2 {
	class Interpreter {
		static char[] whitespace = new char[] { ' ', '\t', '\r' };

		protected interface Op {
			void Do(Interpreter interp);
		}

		public void DoCommand(string[] args) {
			if( args.Length == 0 ) throw new ArgumentException("DoCommand requires at least one thing in the args array");
			if( args[0] == "alias" ) {
				if( args.Length != 3 ) throw new ArgumentException("`alias` requires two arguments: name-to-be-bound, and target-name");
				Console.WriteLine("aliasing "+args[1]+" to "+args[2]);
			}
			Console.WriteLine("Got line: "+System.String.Join(", ",args));
		}
		
		public void HandleLine(string line) {
			line = line.Trim();
			if( line.Length == 0 ) return;
			if( line[0] == '#' ) return;
			string[] parts = line.Split(whitespace, StringSplitOptions.RemoveEmptyEntries);
			DoCommand(parts);
		}
		
		public static void Main(string[] args) {
			string line;
			Interpreter interp = new Interpreter();
			while( (line = Console.ReadLine()) != null ) {
				interp.HandleLine(line);
			}
		}
	}
}
