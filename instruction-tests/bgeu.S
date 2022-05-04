#include "helper.h"

RVTEST_BEGIN
    li t3, 0
    addi t1, t1, 4      # t1=4
    addi t5, t5, 4      # t5=4
    bgeu t1, t5, ge1    # should go to ge1
    addi t1, t1, 4
ge1:
    addi t1, t1, 3
    addi t1, t1, 4
    addi t1, t1, 2      # t1=13
lt1:
    li t3, 6          # t3=6
    addi t1, t1, 2    # t1=15
    bgeu t3, t1, ge2  # should continue
    addi t1, t1, 10
    addi t1, t1, 10
    addi t1, t1, 10
ge2:
    addi t3, zero, -1
    bgeu t3, t1, ge3  # should jump
    nop
    addi t1, t1, 10
    nop
ge3:
    li t2, 45
RVTEST_END
