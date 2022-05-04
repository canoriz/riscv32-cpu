#include "helper.h"

RVTEST_BEGIN
    addi t1, zero, 41                    # t1=4
    addi t2, zero, 56                    # t2=5
    addi t3, zero, 135

    andi t1, t2, 275   # t1 = 16
    andi t4, t3, 743   # t4 = 135
    andi t1, t4, 775   # t1 = 7
    andi t1, t1, 946   # t1 = 2

    li t2, 2
RVTEST_END
