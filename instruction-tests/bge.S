#include "helper.h"

RVTEST_BEGIN
    li t3, 0
    addi t1, t1, 4      # t1=4
    addi t5, t5, 4      # t5=4
    bge  t1, t5, ge1    # should go to ge1
    addi t1, t1, 4
ge1:
    addi t1, t1, 3
    addi t1, t1, 4
    addi t1, t1, 2      # t1=13
lt1:
    li t3, 6          # t3=6
    addi t1, t1, 2    # t1=15
    bge  t3, t1, ne2  # should continue
    addi t1, t1, 10
    addi t1, t1, 10
    addi t1, t1, 10
ne2:
    li t2, 45
RVTEST_END
