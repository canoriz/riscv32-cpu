#include "helper.h"

RVTEST_BEGIN
j1:
    addi t1, t1, 2
    jal repeat
j2:
    addi t1, t1, 1
    jal repeat
j3:
    addi t1, t1, 3
    jal repeat
    j stage2

repeat:
    addi t1, t1, 4
    addi t1, t1, 5
    jalr x0, 0(x1)
    j stage2

jal_t4:
    addi t1, t1, 50
    jalr x0, 0(t4)

stage2:
    # result is 15
    jal t4, jal_t4
    li t2, 83
RVTEST_END