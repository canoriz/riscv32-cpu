#include "helper.h"

RVTEST_BEGIN
    addi t1, zero, 41                    # t1=4
    addi t2, zero, 56                    # t2=5
    addi t3, zero, 135

    add  t1, t2, t3    # t1 = 191
    add  t2, t1, t3    # t2 = 326
    add  t1, t2, t1    # t1 = 517
    li t2, 517
RVTEST_END
