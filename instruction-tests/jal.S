#include "helper.h"

RVTEST_BEGIN
	jal x0, j1
j4:
    addi t1, t1, 4
    addi t1, t1, 5
    j j5
j2:
    addi t1, t1, 2
    j j3
j1:
    addi t1, t1, 1
    j j2
j3:
    addi t1, t1, 3
    j j4

j5:
    # result is 15
    li t2, 15
RVTEST_END