Interpreter.exe: Interpreter.cs
	csc /r:System.Net.Http.dll Interpreter.cs NoCheckCertificatePolicy.cs

.PHONY: test
test: Interpreter.exe
	mono Interpreter.exe <test-program-1.ts34_2
