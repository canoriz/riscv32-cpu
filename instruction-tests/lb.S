#include "helper.h"
# SW and LW are tested together

RVTEST_BEGIN
    li s0, 0x3000
    li t3, 0x12345678
    li t5, 0
    li t4, 0x56
    sw t3, 0(s0)
    lb t5, 1(s0)      # 0x56
    bne t5, t4, ne1   # should continue

    li t3, 0x87654321
    sw t3, 8(s0)
    lb t1, 11(s0)      # sign extend t1=0xffffff87
    li t2, 0xffffff87
    j 1f
ne1:
    li t1, 1
    li t2, 2
1:

RVTEST_END
