#include "helper.h"

RVTEST_BEGIN
    addi t1, zero, 15
    li  t2, 2
    srl t2, t1, t2     # 0b1111 -> 0b11
    li  t1, 3
RVTEST_END
