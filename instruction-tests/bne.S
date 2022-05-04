#include "helper.h"

RVTEST_BEGIN
    li t3, 0
    addi t1, t1, 4      # t1=4
    addi t5, t5, 4      # t5=4
    bne t1, t5, ne1  # should go to eq1
eq1:
    addi t1, t1, 3
    addi t1, t1, 4
    addi t1, t1, 2      # t1=13
ne1:
    li t3, 6
    addi t1, t1, 2
    bne t1, t3, ne2  # should go to ne2
    addi t1, t1, 10
    addi t1, t1, 10
    addi t1, t1, 10
ne2:
    li t2, 15
RVTEST_END
