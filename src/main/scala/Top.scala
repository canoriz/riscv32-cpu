package cpu

import chisel3._
import chisel3.util._
import Consts._

class Top(hexMemoryPath: String) extends Module {
  val io = IO(new Bundle {
    val exit = Output(Bool())
    val gp = Output(UInt(WORD_LEN.W))
    val pc = Output(UInt(WORD_LEN.W))
  })

  val core = Module(new Core())
  val memory = Module(new Memory(hexMemoryPath))

  // Connect ports between core and memory
  core.io.imem <> memory.io.imem
  core.io.dmem <> memory.io.dmem

/*
  core.io.imem.inst  := 0.U(32.W)
  core.io.dmem.rdata := 0.U(32.W)
  memory.io.dmem.wen := false.B
  memory.io.imem.addr:= 0.U(32.W)
  memory.io.dmem.wdata:= 0.U(32.W)
  memory.io.dmem.addr := 0.U(32.W)
  */

  // Connect signals inside core
  io.exit := core.io.exit
  io.gp := core.io.gp
  io.pc := core.io.pc
}
