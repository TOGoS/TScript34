set "cs_source_files=Interpreter.cs NoCheckCertificatePolicy.cs"
set "csc_opts="
:set "csc_opts=/debug:full"

call csc /out:TS34Interpreter.exe %csc_opts% /r:System.Net.Http.dll %cs_source_files%
if errorlevel 1 exit /B 1

call csc /out:LibTest.exe /main:TOGoS.TScrpt34_2.LibTest %csc_opts% /r:System.Net.Http.dll /target:exe LibTest.cs %cs_source_files%
if errorlevel 1 exit /B 1

:mono TS34Interpreter.exe test-program-1.ts34
mono TS34Interpreter.exe encode-demo.ts34
