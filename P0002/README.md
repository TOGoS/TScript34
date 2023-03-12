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
TS34Thiunks could cache various representations of the entity's value,
e.g. "as byte array", etc.  Do those need URIs too, oh no.
Let's put that off for now.

Some value types that can be unambiguously represented by
common data structures in most programming languages:

- Byte array
- Character array or string
- Number (subtypes: integer, floating point, rational, etc)
- Map
- List

Note some representations are included in others.
e.g. if you ask for the 'number' representation, you may get
back any type that represents a number in the host language,
such as a float32 or an integer.

Some 'higher-level' representations:

- RDF object (has an ID, a collection of predicate+object pairs, maybe a 'simple value')
- TS34Thunk
- URI reference
- TOGVM expression

There could be an infinite variety of different representations
of entities/values; we just need to support translating between the representations.
Some thunks may not be translatable to all other representations!

It might make sense to have some methods return 'any object',
e.g. a Java method like `Object getThunkValue(ts34Thunk)` in Java.
Though our TS34Thunk is itself represented as an object in the host language,
it would not be appropriate for `getThunkValue` to return the thunk itself,
unless the thing represented by the thunk happens to be itself.

This sort of, maybe, in my current state of mind, anyway, illustrates
why we must distinguish between representations-of-entities and
representations-of-entity-values.
Or, moreover, entities and their representations!
Or different 'levels' of representation, anyway.

Maybe put another way,  whatever data structure we use,
except for trivial entities that *are* data structures,
can only be interpreted as describing something, not as *being* that thing.
`getThunkValue`'s job is to take the higher-level representation of the entity
and, if there is a data structure that in our system can be reasonably
considered to *be* the entity, return that.  But representations of entities
are themselves entities, so if we treat them as the same, we have introduced ambiguity!

In cases where a thunk cannot be represented by an in-memory data structure,
such as a person, or the set containing all sets, `getThunkValue` should
throw an error.  There may be a a separate, `getThunkUri` function for getting
URIs that identify such abstract concepts, though in some cases they might not
have a name, either!  In which case any logic that wants to reason about
the entity will have to make do with the information in the thunk itself,
or some other representation.

Data structures themselves are a higher-level concept than
what they are made out of, which is bits in memory,
which are represented by tiny physical switches.

Wwitches represent memory, patterns in memory represent data structures,
those data structures can represent concepts that are not themselves data structures.
