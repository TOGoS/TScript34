cs_source_files = Interpreter.cs MapStuff.cs NoCheckCertificatePolicy.cs

.PHONY: all
all: TS34Interpreter.exe TS34Interpreter.dll

TS34Interpreter.exe: ${cs_source_files}
	csc /out:"$@" /debug:full /r:System.Net.Http.dll ${cs_source_files}

TS34Interpreter.dll: ${cs_source_files}
	csc /out:"$@" /debug:full /r:System.Net.Http.dll /target:library ${cs_source_files}

.PHONY: test
test: Interpreter.exe
	mono --debug Interpreter.exe <test-program-1.ts34

.PHONY: clean
clean:
	rm -f Interpreter.exe Interpreter.dll TS34Interpreter.exe TS34Interpreter.dll *.pdb
