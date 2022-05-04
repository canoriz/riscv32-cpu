#include "helper.h"

RVTEST_BEGIN
    addi t1, zero, 4                     # t1=4
    addi t2, zero, 5                     # t2=5
    addi t3, zero, -1                     # t3=5
    addi t4, zero, -2                     # t4=5
    addi t5, zero, 5                     # t5=5
    addi t6, zero, -9                     # t6=5

    li t3, 5
    sltu t2, t2, t3     # 0
    bne t2, zero, 2f

    li t3, -1
    li t2, 1
    sltu t1, t1, t3     # 1
    bne t2, t1, 2f

    li t4, -2
    sltu t3, t3, t4     # 0
    bne t3, zero, 2f

    sltu t4, t4, zero      # 0
    bne t4, zero, 2f

    li t4, -1
    sltu t6, t6, t4     # 1
    bne t2, t6, 2f
    j 1f

2:
    addi t1, zero, 50
1:
    li t2, 1
RVTEST_END
