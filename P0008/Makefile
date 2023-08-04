# Eventually this tool should be able to build itself.
# This Makefile is here for bootstrapping.

target/JCR36-dev.jar: target/classes src/main/meta/META-INF/MANIFEST.MF
	jar -cmf src/main/meta/META-INF/MANIFEST.MF "$@" -C target/classes .

target/classes: $(shell find src/main/java) Makefile
	mkdir -p "$@"
	javac -d "$@" -source 1.6 -target 1.6 $$(find src/main/java -name '*.java')
	touch "$@"
