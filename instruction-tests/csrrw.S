#include "helper.h"
# test CSRRW, using mtvec as CSR

RVTEST_BEGIN
    li    t3, 0x12345678
#case1
    # test no csr forward
    csrrw x0, mtvec, t3  # t3 -> mtvec
    nop
    nop
    nop
    nop
    nop
    csrrs t5, mtvec, x0  # mtvec -> t5
    bne   t3, t5, ne1

#case2
    li    t3, 0x624516
    # test csr forward
    csrrw x0, mtvec, t3  # t3 -> mtvec
    nop
    csrrs t5, mtvec, x0  # mtvec -> t5
    bne   t3, t5, ne1

#case3
    li    t3, 0x6245112
    # test csr forward
    csrrw x0, mtvec, t3  # t3 -> mtvec
    csrrs t5, mtvec, x0  # mtvec -> t5
    bne   t3, t5, ne1

#case4 test write
    li    t3, 0x6
    # test csr forward
    csrrw t4, mtvec, t3  # t3 -> mtvec, t4 = 0x6245112
    csrrs t5, mtvec, x0  # mtvec -> t5, t5 = 0x6
    bne   t3, t5, ne1
    li    t2, 0x6245112
    bne   t2, t4, ne1
    li t2, 0
    li t1, 0

ne1:

RVTEST_END