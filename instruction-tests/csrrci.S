#include "helper.h"
# test CSRRW, using mtvec as CSR

RVTEST_BEGIN
    li    t3, 0x12345678
case1:
    # test no csr forward
    csrrw  x0, mtvec, t3  # t3 -> mtvec
    nop
    nop
    nop
    nop
    nop
    csrrci t5, mtvec, 0x12  # mtvec -> t5
    csrr  t3, mtvec
    li    t5, 0x12345668
    bne   t3, t5, ne1

case2:
    li    t3, 0x624516
    # test csr forward
    csrrw x0, mtvec, t3  # t3 -> mtvec
    nop
    csrrci t5, mtvec, 0x1d  # mtvec -> t5
    nop
    nop
    csrr  t3, mtvec
    li    t5, 0x624502
    bne   t3, t5, ne1

case3:
    li    t3, 0x6245112
    # test csr forward
    csrrw x0, mtvec, t3  # t3 -> mtvec
    csrrci t5, mtvec, 0x1f  # mtvec -> t5
    csrr  t3, mtvec
    li    t5, 0x6245100
    bne   t3, t5, ne1

    li    t2, 0
    li    t1, 0
ne1:

RVTEST_END
