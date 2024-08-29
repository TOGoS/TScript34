rem TODO: Make the project builder invokable from a P0019 script
rem so we don't have to make multiple JAR files

setlocal

set "bootstrap_jar=JavaProjectBuilder-v0.1.2.jar"
if not exist "%bootstrap_jar%" goto need_bootstrap_jar

rem 1.6 is what I want, but my JDKs can't do it

set JAVAC_SOURCE_VERSION=1.7
set JAVAC_TARGET_VERSION=1.7

java -jar "%bootstrap_jar%" -o target/P0019.jar --include-sources --java-sources=src/main/java --main-class=net.nuke24.tscript34.p0019.cmd.P0019Command

goto eof


:need_bootstrap
echo %bootstrap_jar% does not exist.  Download it from wherever.
exit /B 1


:eof
