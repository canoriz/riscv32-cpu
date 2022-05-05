CURRENT_DIR:=$(shell pwd)
RISCV_TESTS := addi
RISCV_ELF := $(patsubst %, riscv-tests/isa/rv32ui-p-%, $(RISCV_TESTS))
RISCV_HEX := $(patsubst %, riscv-tests-hex/%.hex, $(notdir $(RISCV_ELF)))
INSTR_TESTS := addi jal jalr beq bne blt bge bgeu bltu lui slti sltiu xori \
andi ori slli srli sub auipc srai add and or xor slt sltu sll sra srl fence \
sw sh sb lh lb lhu lbu

INSTR_HEX := $(patsubst %, instruction-tests/%.hex, $(INSTR_TESTS))

.PHONY: all test docker dockerimage clean

all:
	$(info $$RISCV_TESTS is [${RISCV_TESTS}])
	$(info $$RISCV_ELF is [${RISCV_ELF}])
	$(info $$RISCV_HEX is [${RISCV_HEX}])
	$(info $$INSTR_HEX is [${INSTR_HEX}])

test: instr-tests
	sbt test

docker:
	docker exec -it myrvcpu bash ||\
	docker start -i myrvcpu ||\
	docker run -it --name myrvcpu -v $(CURRENT_DIR):/app myrvcpu

dockerimage:
	docker build . -t myrvcpu

%.bin: %.elf
	riscv64-unknown-elf-objcopy -O binary $< $@

.PRECIOUS: %.dump
%.dump: %.elf
	riscv64-unknown-elf-objdump -b elf32-littleriscv -d $< > $@

%.hex: %.bin %.dump
	od -An -tx1 -w1 -v $< > $@

instr-tests: $(INSTR_HEX)

#instruction-tests/%.s: instruction-tests/%.c
#	riscv64-unknown-elf-gcc -O0 -march=rv32i -mabi=ilp32 -S -o $@ $<

instruction-tests/%.o: instruction-tests/%.S instruction-tests/helper.h
	riscv64-unknown-elf-gcc -O0 -march=rv32i -mabi=ilp32 -c -o $@ $<

instruction-tests/%.elf: instruction-tests/%.o instruction-tests/link.ld
	riscv64-unknown-elf-ld -b elf32-littleriscv -T ./instruction-tests/link.ld -o $@ $<


# riscv-test from risc-v official repo
riscv-tests-hex/%.bin: riscv-tests/isa/%
	riscv64-unknown-elf-objcopy -O binary riscv-tests/isa/$(notdir $(basename $@)) $@

riscv-tests-hex/%.elf: riscv-tests/isa/%
	cp $< $@

riscv-tests: $(RISCV_HEX)
	@echo $(RISCV_HEX)

clean:
	-rm -rf instruction-tests/*.o
	-rm -rf instruction-tests/*.elf
	-rm -rf instruction-tests/*.bin
	-rm -rf instruction-tests/*.hex
	-rm -rf instruction-tests/*.dump
