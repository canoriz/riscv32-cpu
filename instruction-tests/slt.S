#include "helper.h"

RVTEST_BEGIN
    addi t1, zero, 4                     # t1=4
    addi t2, zero, 5                     # t2=5
    li   t3, 6
    slt  t3, t1, t3                      # t3 = (t1<6) = 1
    addi t6, zero, 1                     # t6=1
    addi t1, zero, -5                    # t1 = -5
    beq  t3, t6, eq1 # should branch     # 1 == 1
    addi t1, zero, 100
eq1:
    li  t3, -2
    slt t3, t1, -2                       # t3 = (-5 < -2) = 1
    beq t3, t6, eq2                      # 1 == 1
    addi t1, zero, 100
eq2:
    li   t3, -100
    slt  t3, t1, t3                      # t3 = (-5 < -100) = 0
    beq  t3, t6, eq3                     # 0 != 1
    addi t2, t1, 0                       # t2 = t1 = -5
    addi t1, t2, 1                       # t1 = t2 = -4
eq3:
    li t2, -4
RVTEST_END
