#include "helper.h"
# dep: slli sub jal

RVTEST_BEGIN
    li t5, 40
    jal t4, 1f         # store pc+4 to t4
1:
    auipc t1, 40      # t1 = pc+(40<<12)
    slli  t5, t5, 12  # t5 = t5<<12
    sub   t1, t1, t5  # sub ui pc
    bne   t1, t4, ne1 # should continue
    addi  t1, zero, 7
ne1:
    li t2, 7
RVTEST_END
