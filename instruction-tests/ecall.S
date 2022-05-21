#include "helper.h"
# test CSRRW, using mtvec as CSR

RVTEST_BEGIN
    ecall
    li t2, 0x536
ne1:

RVTEST_END
