# TScript34-P0017: Rough Design for a Command/Functional Language

2024-05-01, while out on a walk.
Notes in WSITEM-3421, p41-42.

Core idea:
- Expressions look like (and are as compatible as I can manage)
  Scheme expressions, but have no side-effects
- Statements look more like extended TScript34.2 operations,
  or shell / TCL statements.  Statements 'do things', including
  alter the state of the interpreter (e.g. define names used by later
  in the program)
- Expressions can create Actions (analogous to `IO whatever`),
  but not execute them
- Statements combine calling a function with executing;
  `print "foo"` and `execute (print "foo")` are equivalent.

### Use as Data Language

Normally when designing my Scheme dialect,
I worry about the distinction between literal values
and values that must be constructed at runtime.
See EarthIT/timelog/2024/03/29240317-literals.org for
a bunch of thoughts on the subject.
https://srfi.schemers.org/srfi-10/srfi-10.html and
https://srfi.schemers.org/srfi-108/srfi-108.html
are relevant.

However, that crap adds complexity over just saying
"call a constructor".  I am currently thinking that,
especially if expressions are purely functional,
side-effect free, referrentially transparent and all that,
that it's okay to not worry about representing values
directly at the syntax level.

Instead, if you wanted to e.g. parse values from a script,
you would use a procedure that actually evaluates the expressions.
This parser may allow some statements to be used, also.

Perhaps `(foo ...)` should mean `yield (foo ...)`
(meaning, provide this value to the 'read-values-from' function, or whatever)
though this would only be for convenience and adds some ambiguity.
