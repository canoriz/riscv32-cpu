#include "helper.h"

RVTEST_BEGIN
    li t3, 0
    addi t1, t1, 4
    addi t5, t5, 4
    # should branch
    beq t1, t5, right1
wrong1:
    nop
    nop
    addi t3, t1, 2
right1:
    addi t1, t1, 2
    # should not branch
    beq t1, t3, wrong2
    addi t1, t1, 10
    addi t1, t1, 10
    addi t1, t1, 10
wrong2:
    li t2, 36
RVTEST_END
