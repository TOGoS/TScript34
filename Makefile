# Eventually this tool should be able to build itself.
# This Makefile is here for bootstrapping.

target/classes: $(shell find src/main/java) Makefile
	mkdir -p "$@"
	javac -d "$@" -source 1.6 -target 1.6 $$(find src/main/java -name '*.java')
	touch "$@"
