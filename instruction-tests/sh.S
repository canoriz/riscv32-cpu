#include "helper.h"

RVTEST_BEGIN
    li s0, 0x3000
    li t3, 0x12345678
    li t5, 0x43218765
    sw t3, 0(s0)
    sh t5, 0(s0)      # 0x12348765
    lw t5, 0(s0)      # immediate get
    li t3, 0x12348765
    bne t3, t5, ne1   # should continue

    li t3, 0x87654321
    sw t3, 8(s0)
    sh t3, 10(s0)     # 0x43214321
    addi s0, s0, 4    # try add then get
    lw t5, 4(s0)
    li t3, 0x43214321
    bne t3, t5, ne1   # should continue
    li t1, 0
    li t2, 0
    j 1f
ne1:
    li t1, 1
    li t2, 2
1:

RVTEST_END
