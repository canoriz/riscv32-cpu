#include "helper.h"

RVTEST_BEGIN
    addi t1, zero, 4
    addi t2, zero, 5
    addi t1, t2, 6
    addi t1, t2, 7  # t1 = 5 + 7 = 12
    addi t1, t1, 7  # t1 = 12 + 7 = 19
    addi t4, t1, 1
    addi t1, t4, -1
    li t2, 19
RVTEST_END
