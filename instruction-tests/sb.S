#include "helper.h"

RVTEST_BEGIN
    li s0, 0x3000
    li t3, 0x12345678
    li t5, 0x43218765
    sw t3, 0(s0)
    sb t5, 0(s0)      # 0x12345665
    lw t5, 0(s0)      # immediate get
    li t3, 0x12345665
    bne t3, t5, ne1   # should continue

    li t3, 0x87654321
    sw t3, 8(s0)
    sb t3, 11(s0)     # 0x21654321
    addi s0, s0, 4    # try add then get
    lw t5, 4(s0)
    li t3, 0x21654321
    bne t3, t5, ne1   # should continue

    li t3, 0x87654321
    sw t3, 8(s0)
    sb t3, 9(s0)     # 0x87652121
    lw t5, 8(s0)
    li t3, 0x87652121
    bne t3, t5, ne1   # should continue

    li t1, 0
    li t2, 0
    j 1f
ne1:
    li t1, 1
    li t2, 2
1:

RVTEST_END
