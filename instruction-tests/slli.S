#include "helper.h"

RVTEST_BEGIN
    addi t1, zero, 4 # 0b100 -> 0b10000
    slli t2, t1, 2
    li t1, 16
RVTEST_END
