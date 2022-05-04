#include "helper.h"

RVTEST_BEGIN
    addi t1, zero, 4 # 0b100 -> 0b10000
    li  t3, 2
    sll t2, t1, t3
    li  t1, 16
RVTEST_END
