/*
 The test assembly's helper macros

 Suppose j(jal), addi, bne is ok write testing assembly code between
 RVTEST_BEGIN and RVTEST_END

 The result stores in t1, expected stores t2. Macro compares t1 and t2
 set gp 1 if t1==t2, else set gp 2
*/
 
#define RVTEST_BEGIN ;\
.global _asm_start; \
_asm_start:;


#define RVTEST_END ;\
bne t1, t2, test_fail; \
test_pass:; \
	addi gp, x0, 1; \
    j loop4ever; \
test_fail:; \
    addi gp, x0, 2; \
loop4ever:; \
    j loop4ever;
