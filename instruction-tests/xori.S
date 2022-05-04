#include "helper.h"

RVTEST_BEGIN
    addi t1, zero, 41                    # t1=4
    addi t2, zero, 56                    # t2=5
    addi t3, zero, 135

    xori t1, t2, 275   # t1 = 299
    xori t4, t3, 743   # t4 = 608
    xori t1, t4, 775   # t1 = 359
    xori t1, t1, 946   # t1 = 725

    li t2, 725
RVTEST_END
