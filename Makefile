.PHONY: default
default: target/TS34P19-dev.jar

.PHONY: clean
clean:
	rm -rf target .temp

target:
	mkdir target

target/java-sources.lst: $(shell find src) | target
	find src -name "*.java" >"$@"

.DELETE_ON_ERROR: target/.classes-built
target/.classes-built: target/java-sources.lst
	rm -rf target/classes
	mkdir -p target/classes
	javac -source 1.6 -target 1.6 -d target/classes @target/java-sources.lst
	mkdir -p "target/classes/META-INF"
	echo "Manifest-Version: 1.0" > "target/classes/META-INF/MANIFEST.MF"
	echo "Main-Class: net.nuke24.tscript34.p0019.cmd.P0019Command" >> "target/classes/META-INF/MANIFEST.MF"
	touch "$@"

.DELETE_ON_ERROR: target/TS34P19-dev.jar
target/TS34P19-dev.jar: target/.classes-built
	cd target/classes && zip ../../$@ -r .
