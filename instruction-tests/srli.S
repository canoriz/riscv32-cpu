#include "helper.h"

RVTEST_BEGIN
    addi t1, zero, 15
    srli t2, t1, 2     # 0b1111 -> 0b11
    li t1, 3
RVTEST_END
