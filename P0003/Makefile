.PHONY: all
all:
	@echo "No default make rule.  Maybe make fun?"

.PHONY: fun
fun: target/psified-tsv.ps

.PHONY: run-readlines
run-readlines:
	@echo "Running readlines.ps on its own source code:"
	@gs -dQUIET -dNODISPLAY -dBATCH src/main/ps/readlines.ps < src/ps/readlines.ps


target/psified-tsv.ps: src/test/resources/a-tsv-file.tsv src/main/ps/parsetsv.ps
	mkdir -p target
	gs -dQUIET -dNODISPLAY -dBATCH src/main/ps/parsetsv.ps < "$<" > "$@"
