Interpreter.exe: Interpreter.cs
	csc Interpreter.cs

.PHONY: test
test: Interpreter.exe
	mono Interpreter.exe <test-program-1.ts34_2
