package net.nuke24.tscript34.p0011.sexp;

public class Symbols {
	private Symbols() { }
	
	public static final String S_NIL = "http://ns.nuke24.net/TScript34/P0011/Values/Nil";
	public static final String S_MACRO = "http://ns.nuke24.net/TScript34/P0011/X/Macro";
	public static final String FN_CONCAT = "http://ns.nuke24.net/TOGVM/Functions/Concatenate";
	public static final String FN_CONS = "http://ns.nuke24.net/TScript34/P0011/Functions/Cons";
	public static final String FN_HEAD = "http://ns.nuke24.net/TScript34/P0011/Functions/Head";
	public static final String FN_TAIL = "http://ns.nuke24.net/TScript34/P0011/Functions/Tail";
	public static final String MN_LAMBDA = "http://ns.nuke24.net/TScript34/P0011/Macros/Lambda";
	public static final String MN_QUOTE  = "http://ns.nuke24.net/TScript34/P0011/Macros/Quote";
	
	public static final Atom NIL = new Atom(S_NIL);
	
	public static boolean isSymbol(Object obj, String text) {
		return obj instanceof Atom && text.equals(((Atom)obj).text);
	}
	public static boolean isNil(Object obj) {
		return isSymbol(obj, S_NIL);
		
	}
}
