@echo off

if defined CSI_EXE goto run

if exist "C:\tools\chicken\bin\csi.exe" (
	set "CSI_EXE=C:\tools\chicken\bin\csi.exe"
	goto run
)

echo CSI_EXE not defined and I don't know where to look for it.>&2
exit/B 1

:run
%CSI_EXE% -ss src\interp01\chicken\run-tests.scm <test-cases.tsv
