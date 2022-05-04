#include "helper.h"
# fence, same as nop now

RVTEST_BEGIN
    li t1, 10
    fence
    fence io, io
    fence.i
    fence.tso
    li t2, 10
RVTEST_END
