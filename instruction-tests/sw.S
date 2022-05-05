#include "helper.h"
# SW and LW are tested together

RVTEST_BEGIN
    li s0, 0x3000
    li t3, 0x12345678
    li t5, 0
    sw t3, 0(s0)
    nop
    nop
    nop
    lw t5, 0(s0)      # immediate get
    bne t3, t5, ne1   # should continue

    li t3, 0x87654321
    sw t3, 8(s0)
    addi s0, s0, 4    # try add then get
    lw t1, 4(s0)      # t1=0x87654321
    addi t1, t1, 4    # read after load
    li t2, 0x87654325
    j 1f
ne1:
    li t1, 1
    li t2, 2
1:

RVTEST_END
