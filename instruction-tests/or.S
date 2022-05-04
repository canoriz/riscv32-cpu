#include "helper.h"

RVTEST_BEGIN
    addi t1, zero, 841
    addi t2, zero, 576
    addi t3, zero, 245

    or  t1, t2, t3    # t1 = 757
    li t2, 757
RVTEST_END
