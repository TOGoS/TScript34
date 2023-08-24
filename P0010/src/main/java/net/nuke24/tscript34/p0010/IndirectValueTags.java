package net.nuke24.tscript34.p0010;

public class IndirectValueTags
{
	// These numbers are the 't' component of urn:oid:1.3.6.1.4.1.44868.261.34.10.t
	
	//  0..15 are reserved for the most common and generic tags:
	
	public static final int NULL  = 0;
	public static final int TRUE  = 1;
	public static final int FALSE = 2;
	/** The tagged object itself, with no semantics whatsoever aside from being equal only to itself and nothing else */
	public static final int ANONYMOUS_CONCEPT = 3;
	/**
	 * A concept named by Supplier#get(), the 'reference',
	 * which is probably, but not necessarily, a String.
	 * 
	 * Has the connotation that this concept cannot be meaningfully represented
	 * any more concretely than by this name.  Useful for representing, e.g.
	 * your cousin Bob using a URN.  'resolving' a PURE_REFERENCE-tagged IndirectValue
	 * can be expected to return the same IndirectValue.
	 * 
	 * Similar to 'keywords' in Scheme, except that the reference does not include the ':' or '#:'
	 * which distinguishes keywords from symbols in scheme.
	 */
	public static final int PURE_REFERENCE = 4;
	/**
	 * A concept named by Supplier#get(), the 'reference',
	 * which is probably, but not necessarily, a String.
	 * 
	 * Semantics are identical to those of PURE_REFERENCE with the exception
	 * that REFERENCE leaves open the possibility of 'resolving' the value,
	 * though the result may be simply a PURE_REFERENCE with the same name!
	 */
	public static final int REFERENCE = 5;
	/**
	 * Supplier#get() returns the literal value, which must be interpreted
	 * as exactly itself, even if it is itself an IndirectValue.
	 * Objects referenced by this object should, however,
	 * be treated to the usual 'IndirectValue should be interpreted specially' rule.
	 */
	public static final int QUOTED_LITERAL = 6;
	/**
	 * Supplier#get() returns the literal value, which must be interpreted
	 * as exactly itself, even if it is itself an IndirectValue.
	 * Any objects contained within should *also* be interpreted as literally themself,
	 * even if they are IndirectValue instances.
	 * This may be tricky to implement, as it may require a flag
	 * to be passed to functions that process these objects.
	 */
	public static final int QUOTED_LITERAL_RECURSIVE = 7;
	
	// 8.. are list types
	public static final int EMPTY_LIST  = 8;
	/**
	 * Pair#getLeft() returns the head, Pair#getRight() returns the tail
	 * e.g. (first . (second . (third . ())
	 **/
	public static final int LINKED_LIST = 9;
	
	// 16..1023 are reserved to identify things other than these tags.
	
	// 1024..1039 are concatenation types, where 1024 is the most generic,
	// and low bits can be set to non-zero values to hint about
	// element type and representation; implementations may choose
	// to use/support the lower bits or not:
	/**
	 * A sequence defined by some number of subsequences to be concatenated together
	 * get() should return a Pair, an array, or a List.
	 */
	public static final int CONCATENATION = 1024;
	// More specific concatenations: 1024 + <representation type:2 bits> <element type:2 bits>
	// Sequence representations:
	public static final int SR_UNDEFINED = 0;
	public static final int SR_PAIR      = 1;
	public static final int SR_ARRAY     = 2;
	public static final int SR_LIST      = 3;
	public static final int ET_UNDEFINED = 0;
	public static final int ET_BYTE      = 1;
	public static final int ET_CHAR      = 2;
	private static final int ET_TYPE3 = 3; // i.e. reserved
	private static final int CONCATENATION_LIST_OF_TYPE3 = 1039;
	
	/**
	 * List (represented as a Pair, String[], Object[], or List) of [data, datatype URI, ...]
	 * as per https://www.nuke24.net/docs/2023/TS34EncodedDatatype.html.
	 * 
	 * First element is the encoded representation of the value.
	 * It will often be, but does not need to be, a String.
	 * Remaining elements name encodings that have been applied,
	 * from outermost to innermost.  i.e. in order such that unapplying
	 * the encodings in order would return the original value.
	 * 
	 * e.g., the following all represent the same thing, the number 123:
	 * - ("data:,123" . ("http://ns.nuke24.net/Datatypes/URIResource" . ("http://www.w3.org/2001/XMLSchema#decimal" . ())))
	 * - ("123" . ("http://www.w3.org/2001/XMLSchema#decimal" . ()))
	 * - (123 . ())
	 * - String[] { "data:,123", "http://ns.nuke24.net/Datatypes/URIResource", "http://www.w3.org/2001/XMLSchema#decimal"}
	 * - Arrays.asList("123","http://www.w3.org/2001/XMLSchema#decimal")
	 */
	public static final int TS34_ENCODED = 1040;
	/**
	 * Supplier#get() returns some representation of a functional expression
	 * (probably TOGVM-compatible) that can be unambiguously evaluated
	 * to provide the value.
	 */
	public static final int EXPRESSION_VALUE = 1040;
}
