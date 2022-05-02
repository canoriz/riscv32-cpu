	.file	"jal.c"
	.option nopic
	.option checkconstraints
	.attribute arch, "rv32i2p0_v2p0"
	.attribute unaligned_access, 0
	.attribute stack_align, 16
	.text
	.section	.text.startup,"ax",@progbits
	.align	2
	.globl	main
	.type	main, @function
main:
 #APP
# 3 "instruction-tests/jal.c" 1
	jal x1, jump
# 0 "" 2
# 4 "instruction-tests/jal.c" 1
	nop
# 0 "" 2
# 6 "instruction-tests/jal.c" 1
	addi x5, x0, 1
# 0 "" 2
# 7 "instruction-tests/jal.c" 1
	jump:
# 0 "" 2
# 8 "instruction-tests/jal.c" 1
	addi x2, x0, 1
# 0 "" 2
 #NO_APP
	li	a0,0
	ret
	.size	main, .-main
	.ident	"GCC: (GNU) 9.2.0"
