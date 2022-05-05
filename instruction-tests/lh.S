#include "helper.h"
# SW and LW are tested together

RVTEST_BEGIN
    li s0, 0x3000
    li t3, 0x12345678
    li t5, 0
    li t4, 0x5678
    sw t3, 0(s0)
    lh t5, 0(s0)      # 0x5678
    bne t5, t4, ne1   # should continue

    li t3, 0x87654321
    sw t3, 8(s0)
    lh t1, 10(s0)      # sign extend t1=0xffff8765
    li t2, 0xffff8765
    j 1f
ne1:
    li t1, 1
    li t2, 2
1:

RVTEST_END
