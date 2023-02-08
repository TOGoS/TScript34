cs_source_files = Interpreter.cs MapStuff.cs NoCheckCertificatePolicy.cs

Interpreter.exe: ${cs_source_files}
	csc /r:System.Net.Http.dll ${cs_source_files}

.PHONY: test
test: Interpreter.exe
	mono Interpreter.exe <test-program-1.ts34_2
