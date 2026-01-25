
## 2026-01-24

I'm trying to install this very experimental Scheme LSP that supposedly supports Chicken:
https://codeberg.org/rgherdt/scheme-lsp-server#a-name-user-content-installing-a-installing

Running some stuff as admin, as instructed
(running as not-admin resulted in 'permission denied',
since Chicken was installed via Chocolatey and therefore
lives in a system-wide admin-owned directory):

```cmd
cd C:\tools\chicken\share\chicken
curl http://3e8.org/pub/chicken-doc/chicken-doc-repo.tgz | tar zx
chicken-install -s apropos chicken-doc srfi-18

rem Lots of compilation happens at this point.  It takes a while.
rem And eventually quits with "Error: unterminated list"

chicken-install lsp-server

rem Does some stuff and again "Error: unterminated list"
```

So that all failed.  Scrap this LSP business for now.
