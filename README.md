## Syntax

A a MINIMAL, multi-platform, PostScript-compatible language,
for use as a compiler target, or for writing parsers.

Hopefully mostly-postscript-compatible.

i.e. a subset of programs are valid postscript and TScript26.

It 

```
123 % number literal
(foo bar baz) % a string
immediately-evaluated-name
/literal-name
{ add 2 div } % procedure
[ /foo bar 123 (baz quux) ] % an array
dup exch pop copy roll index mark clear count counttomark cleartomark % standard stack operations
add sub mul div idiv mod % double-argument arithmetic functions
abs neg ceiling floor truncate % single-argument arithmetic functions
sqrt exp ln log sin cos atan % single-argument trig functions
rand srand rrand % PRNG functions
# etc, etc...
```

extensions:
- "#!" or "# ", outside a literal string, following beginning of line or whitespace start a comment. 