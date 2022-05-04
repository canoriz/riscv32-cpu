#include "helper.h"

RVTEST_BEGIN
    addi t1, zero, -15
    li   t2, 2
    sra  t2, t1, t2
    li t1, -4
RVTEST_END
