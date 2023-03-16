cs_source_files = Interpreter.cs NoCheckCertificatePolicy.cs
csc_opts = /debug:full

.PHONY: all
all: TS34Interpreter.exe

TS34Interpreter.exe: ${cs_source_files}
	csc /out:"$@" ${csc_opts} /r:System.Net.Http.dll ${cs_source_files}

LibTest.exe: LibTest.cs ${cs_source_files}
	csc /out:"$@" /main:TOGoS.TScrpt34_2.LibTest ${csc_opts} /r:System.Net.Http.dll /target:exe LibTest.cs ${cs_source_files}

.PHONY: test
test: TS34Interpreter.exe
	mono --debug TS34Interpreter.exe <test-program-1.ts34

.PHONY: clean
clean:
	rm -f Interpreter.exe Interpreter.dll TS34Interpreter.exe TS34Interpreter.dll *.pdb
