# JavaCommandrunner36

A small, retro-ish (runs on JDK 1.6) Java program for running commands
to spawn processes and other suchlike in a more platform-agnostic way
than $SHELL allows.

Command syntax:

```
jcr:docmd [<option> ...] [<var>=<value> ...] [--] <program> [<arg> ...]
```

When run on the command-line, `jcr:docmd` is implied.

## Commands / 'action constructors'

|  Default    |                                                         |
|   alias     |  Full name                                              |
|-------------|---------------------------------------------------------|
| `jcr:docmd` | `http://ns.nuke24.net/JavaCommandRunner36/Action/DoCmd` |
| `jcr:exit`  | `http://ns.nuke24.net/JavaCommandRunner36/Action/Exit`
| `jcr:print` | `http://ns.nuke24.net/JavaCommandRunner36/Action/Print` |
| `jcr:runsys` | `http://ns.nuke24.net/JavaCommandRunner36/Action/RunSysProc` |
