set "cs_source_files=Interpreter.cs NoCheckCertificatePolicy.cs"
set "csc_opts="
:set "csc_opts=/debug:full"

call csc /out:TS34Interpreter.exe %csc_opts% /r:System.Net.Http.dll %cs_source_files%

call csc /out:LibTest.exe /main:TOGoS.TScrpt34_2.LibTest %csc_opts% /r:System.Net.Http.dll /resource:TS34Interpreter.exe /target:exe LibTest.cs
