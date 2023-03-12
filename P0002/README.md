# TScript34.2

A dotNET implementation of a stack-based VM with
a super simplistic one-op-per-line textual representation.

## A thought: TS34Thunk

Passing all values as expressions means that anything can be used as an LValue.

In practice, don't want to go around having random expressions being treated as lvalues;
that would be too surprising.

But maybe it simplifies the implementation?  Idk.

Really they're 'L-expressions', not L-values.
Expressions don't always have (known) values.

Maybe I'll call the things that get passed around, uh, 'concepts'?
*checks thesaurus*
eh, how about 'entity'?
Well, that's all well and good for referring to the abstract things
we're talking about, like a file, but how about a word for the data structure
that represents them?  A TS34Thunk?  Sure.

And also, I have 'TS34Encoding'; don't need "expressions"
so much as `[data encoding ...]` lists.
TS34Thiunks could cache various representations of the entity,
e.g. "as byte array", etc.  Do those need URIs too, oh no.
Let's put that off for now.

Some in-memory representations of objects
off the top of my head:

- Byte array
- Character array or string
- Number (subtypes: integer, floating point, rational, etc)
- RDF object (has an ID, a collection of predicate+object pairs, maybe a 'simple value')
- Map
- List
- TS34Thunk
- URI reference
- TOGVM expression

NOTE THAT TS34THUNK IS ITSELF *JUST ANOTHER REPRESENTATION*.
Albiet one that can theoretically represent all objects,
unlike e.g. a byte array, which is limited to...byte arrays.
Unless some encoding is indicated.  That's what TS34Thinks are for.

There could be an infinite variety.
We just need to support translating between TS34Thunk
and any other that we want to support.
Some thunks may not be translatable to all other representations!
