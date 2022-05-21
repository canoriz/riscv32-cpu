/*
 The test assembly's helper macros

 Suppose j(jal), addi, bne is ok write testing assembly code between
 RVTEST_BEGIN and RVTEST_END

 The result stores in t1, expected stores t2. Macro compares t1 and t2
 set gp 1 if t1==t2, else set gp 2
*/

#define RVTEST_BEGIN ;\
.global _asm_start; \
_asm_start:;\
RVTEST_SET_TRAP_BASE \
real_code:; \
addi t1, zero, 0; \
addi t2, zero, 1;


#define RVTEST_END ;\
bne t1, t2, test_fail; \
test_pass:; \
	addi gp, x0, 1; \
    j loop4ever; \
test_fail:; \
    addi gp, x0, 2; \
loop4ever:; \
    j loop4ever;

// set trap base
#define RVTEST_SET_TRAP_BASE ;\
la    t1, trap_base; \
csrw  mtvec, t1; \
j     real_code; \
\
trap_base:; \
csrr  t1, mepc; \
addi  t1, t1, 4; \
csrw  mepc, t1; \
addi  t1, zero, 0x536; \
mret;
