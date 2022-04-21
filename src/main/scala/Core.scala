package cpu

import chisel3._
import chisel3.util._
import Consts._
import Instructions._

//         ┌─┐          ┌─┐          ┌─┐           ┌─┐
// ┌────┐  │ │  ┌────┐  │ │  ┌────┐  │ │  ┌─────┐  │ │  ┌────┐
// │    │  │ │  │    │  │ │  │    │  │ │  │     │  │ │  │    │
// │ IF ├─►│ ├─►│ ID ├─►│ ├─►│ EX ├─►│ ├─►│ MEM ├─►│ ├─►│ WB │
// │    │  │ │  │    │  │ │  │    │  │ │  │     │  │ │  │    │
// └────┘  │ │  └────┘  │ │  └────┘  │ │  └─────┘  │ │  └────┘
//         └─┘          └─┘          └─┘           └─┘
//        IF/ID        IF/ID        IF/MEM       MEM/ID
//      registers    registers    registers     registers

class Core extends Module {
  val io = IO(new Bundle {
    // Flip input and output to connect to memory
    val imem = Flipped(new ImemPortIo())
    val dmem = Flipped(new DmemPortIo())

    // This port signal will be `true` when a program finished
    val exit = Output(Bool())
    val gp = Output(UInt(WORD_LEN.W)) // for riscv-tests
    val pc = Output(UInt(WORD_LEN.W)) // for riscv-tests
  })

  // RISC-V has 32 registers. Size is 32bits (32bits * 32regs).
  val regfile = Mem(32, UInt(WORD_LEN.W))
  // Control and Status Registers
  val csr_regfile = Mem(4096, UInt(WORD_LEN.W))
  // 128bit Vector Registers
  val vec_regfile = Mem(32, UInt(VLEN.W))

  val pc = 0.U(WORD_LEN.W)

  io.imem.addr  := pc
  io.dmem.addr  := pc
  io.dmem.wen   := false.B
  io.dmem.wdata := pc

  //io.dmem.rdata := pc
  //io.imem.inst  := pc

  //val fetch = new FetchStage()
  //val decode = new DecodeStage()
  //val execute = new ExecuteStage()
  //val mem = new MemStage()
  //val wb = new WriteBackStage()

  //fetch.connect(io.imem, execute, csr_regfile)
  //decode.connect(fetch, execute, mem, regfile)
  //execute.connect(decode)
  //mem.connect(io.dmem, execute, decode, csr_regfile)
  //wb.connect(mem, regfile, vec_regfile)

  // We can know that a program is exiting when it is jumping to the current address. This never
  // happens in C source since C does not allow an infinite loop without any side effect. The
  // infinite loop is put in start.s.
  //
  //    00000008 <_loop>:
  //       8:   0000006f                j       8 <_loop>
  //
  // This seems a normal way to represent a program exits. GCC generates a similar code in _exit
  // function (eventually called when a program exists).
  //
  // 0000000000010402 <_exit>:
  //    ...
  //    10410:       00000073                ecall
  //    10414:       00054363                bltz    a0,1041a <_exit+0x18>
  //    10418:       a001                    j       10418 <_exit+0x16>
  //    ...
  //    10426:       008000ef                jal     ra,1042e <__errno>
  //    1042a:       c100                    sw      s0,0(a0)
  //    1042c:       a001                    j       1042c <_exit+0x2a>
  //
  io.exit := true.B

  io.gp := regfile(3)
  io.pc := 0.U(32.W)

  //printf(p"dmem: addr=${io.dmem.addr} wen=${io.dmem.wen} wdata=0x${Hexadecimal(io.dmem.wdata)}\n") // memory address loaded by LW

  when(io.exit) {
    printf(p"returned from main with ${regfile(10)}\n") // x10 = a0 = return value or function argument 0
  }
  printf("----------------\n")
}
