#include "helper.h"

RVTEST_BEGIN
    addi t1, zero, 41                    # t1=4
    addi t2, zero, 56                    # t2=5
    addi t3, zero, 135

    sub  t1, t2, t3    # t1 = -79
    sub  t2, t1, t3    # t2 = -214
    sub  t1, t2, t1    # t1 = -135
    li t2, -135
RVTEST_END
