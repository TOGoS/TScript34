rem TODO: Make the project builder invokable from a P0019 script
rem so we don't have to make multiple JAR files

setlocal

rem 1.6 is what I want, but my JDKs can't do it

set JAVAC_SOURCE_VERSION=1.7
set JAVAC_TARGET_VERSION=1.7

java -jar JavaProjectBuilder-v0.1.0.jar -o target/JavaProjectBuilder.jar --include-sources --java-sources=src/main/java --main-class=net.nuke24.tscript34.p0019.util.JavaProjectBuilder
java -jar JavaProjectBuilder-v0.1.0.jar -o target/P0019.jar --include-sources --java-sources=src/main/java --main-class=net.nuke24.tscript34.p0019.P0019
