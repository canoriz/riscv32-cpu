CURRENT_DIR:=$(shell pwd)

.PHONY: all test docker dockerimage clean
test:
	sbt test

docker: dockerimage
	docker exec -it myrvcpu bash ||\
	docker start -i myrvcpu ||\
	docker run -it --name myrvcpu -v $(CURRENT_DIR):/app myrvcpu

dockerimage:
	docker build . -t myrvcpu

#instruction-tests/%.s: instruction-tests/%.c
#	riscv64-unknown-elf-gcc -O2 -march=rv32iv -mabi=ilp32 -S -o $@ $<

instruction-tests/%.o: instruction-tests/%.s
	riscv64-unknown-elf-gcc -O2 -march=rv32iv -mabi=ilp32 -c -o $@ $<

instruction-tests/%.elf: instruction-tests/%.o instruction-tests/crt0.o instruction-tests/link.ld
	riscv64-unknown-elf-ld -b elf32-littleriscv -T ./instruction-tests/link.ld -o $@ $< ./instruction-tests/crt0.o

%.bin: %.elf
	riscv64-unknown-elf-objcopy -O binary $< $@

.PRECIOUS: %.dump
%.dump: %.elf
	riscv64-unknown-elf-objdump -b elf32-littleriscv -D $< > $@

%.hex: %.bin %.dump
	od -An -tx1 -w1 -v $< > $@

clean:
	-rm -rf instruction-tests/*.o
	-rm -rf instruction-tests/*.elf
	-rm -rf instruction-tests/*.bin
	-rm -rf instruction-tests/*.hex
	-rm -rf instruction-tests/*.dump
