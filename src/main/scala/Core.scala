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

  val pc = RegInit(0.U(WORD_LEN.W))

  io.imem.addr  := pc
  io.dmem.addr  := pc
  io.dmem.wen   := false.B
  io.dmem.wdata := pc
  printf(p"pc is 0x${Hexadecimal(pc)}\ninstruction is 0x${Hexadecimal(io.imem.inst)}\n")

  pc := pc + 4.U(WORD_LEN.W)
  io.exit := (pc >= 48.U(WORD_LEN.W))

  io.gp := regfile(3)
  io.pc := pc

  //printf(p"dmem: addr=${io.dmem.addr} wen=${io.dmem.wen} wdata=0x${Hexadecimal(io.dmem.wdata)}\n") // memory address loaded by LW

  when(io.exit) {
    printf(p"returned from main with ${regfile(10)}\n") // x10 = a0 = return value or function argument 0
  }
  //printf("----------------\n")
}
