#include "helper.h"

RVTEST_BEGIN
    addi t1, zero, 417
    addi t2, zero, 1023
    addi t3, zero, 135

    and  t4, t2, t3    # t1 = 135
    and  t2, t1, t3    # t2 = 129
    and  t1, t2, t1    # t1 = 129
    li t2, 129
RVTEST_END
