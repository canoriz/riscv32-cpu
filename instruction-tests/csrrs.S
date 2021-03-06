#include "helper.h"
# test CSRRW, using mtvec as CSR

RVTEST_BEGIN
    li    t3, 0x12345678
case1:
    # test no csr forward
    csrrw x0, mtvec, t3  # t3 -> mtvec
    li    t2, 0x62ad52
    csrrs t5, mtvec, t2  # mtvec -> t5
    nop
    nop
    nop
    nop
    nop
    csrr  t3, mtvec # t3 = 0x1276ff7a
    li    t5, 0x1276ff7a
    bne   t3, t5, ne1

case2:
    li    t3, 0x624516
    # test csr forward
    csrrw x0, mtvec, t3  # t3 -> mtvec
    nop
    li    t2, 0x62ad52
    csrrs t5, mtvec, t2  # mtvec -> t5
    nop
    nop
    csrr  t3, mtvec # t3 = 0x62ed56
    li    t5, 0x62ed56
    bne   t3, t5, ne1

case3:
    li    t3, 0x6245112
    # test csr forward
    csrrw x0, mtvec, t3  # t3 -> mtvec
    li    t2, 0x62ad52
    csrrs t5, mtvec, t2  # mtvec -> t5
    csrr  t3, mtvec # t3 = 0x666fd52
    li    t5, 0x666fd52
    bne   t3, t5, ne1

    li    t2, 0
    li    t1, 0
ne1:

RVTEST_END
