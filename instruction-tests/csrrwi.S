#include "helper.h"
# test CSRRW, using mtvec as CSR

RVTEST_BEGIN
#case1
    # test no csr forward
    csrrwi x0, mtvec, 0x12  # imm -> mtvec
    nop
    nop
    nop
    nop
    nop
    csrr  t5, mtvec  # mtvec -> t5
    li    t3, 0x12
    bne   t3, t5, ne1

#case2
    csrrwi x0, mtvec, 0x4  # imm -> mtvec
    nop
    nop
    csrr  t5, mtvec  # mtvec -> t5
    li    t3, 0x4
    bne   t3, t5, ne1

#case3
    # test csr forward
    csrrwi x0, mtvec, 0xc  # t3 -> mtvec
    csrrs  t5, mtvec, x0  # mtvec -> t5
    li     t3, 0xc
    bne    t3, t5, ne1

#case4 test write
    # test csr forward
    csrrwi t4, mtvec, 0x6  # t3 -> mtvec, t4 = 0xc
    csrrs  t5, mtvec, x0  # mtvec -> t5, t5 = 0x6
    li     t3, 6
    bne    t3, t5, ne1
    li     t2, 0xc
    bne    t2, t4, ne1
    li     t2, 0
    li     t1, 0

ne1:

RVTEST_END
