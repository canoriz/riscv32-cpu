#include "helper.h"

RVTEST_BEGIN

    lui   t1, 0xfffff   # t1 = 0xfffff000
    addi  t1, t1, 1365
    addi  t1, t1, 1365
    addi  t1, t1, 1365  # t1 = 0xffffffff = -1
    li    t3, -1
    bne   t1, t3, ne1   # should continue
    addi  t1, t1, 10
ne1:
    li    t2, 9
RVTEST_END
