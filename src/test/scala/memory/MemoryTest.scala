package cpu

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.BundleLiterals._

import Consts._

/**
  * This is a memory IO test
  * From within sbt use:
  * {{{
  * testOnly gcd.GcdDecoupledTester
  * }}}
  */
class MemoryTest extends AnyFlatSpec with ChiselScalatestTester {

  "I-memory read m16k_1" should "pass" in {
    test(new Memory("src/test/scala/resources/memory/m16k_1.hex")) { dut =>
      val expectRdata =Seq(0x6da82cadL, 0xe5d9d38fL, 0xda48fe6fL, 0x9a5e0e9fL).
        map(x => x.U(WORD_LEN.W))
      for (i <- 0 until 4) {
        dut.io.imem.addr.poke((4*i).U(WORD_LEN.W))
        dut.io.imem.inst.expect(expectRdata(i))
      }
    }
  }

  "D-memory read m16k_1" should "pass" in {
    test(new Memory("src/test/scala/resources/memory/m16k_1.hex")) { dut =>
      val expectRdata =Seq(0x6da82cadL, 0xe5d9d38fL, 0xda48fe6fL, 0x9a5e0e9fL).
        map(x => x.U(WORD_LEN.W))
      for (i <- 0 until 4) {
        dut.io.dmem.addr.poke((4*i).U(WORD_LEN.W))
        dut.io.dmem.rdata.expect(expectRdata(i))
      }
    }
  }


  "D-memory write m16k_1" should "pass" in {
    test(new Memory("src/test/scala/resources/memory/m16k_1.hex")) { dut =>
      val writeData =Seq(0x01020304L, 0x05060708L, 0x090a0b0cL, 0xd0e0f000L).
        map(x => x.U(WORD_LEN.W))
      for (i <- 0 until 4) {
        dut.io.dmem.addr.poke((4*i).U(WORD_LEN.W))
        dut.io.dmem.wdata.poke(writeData(i))
        dut.io.dmem.wen.poke(true.B)
        dut.clock.step()
        dut.io.dmem.rdata.expect(writeData(i))
      }
    }
  }
}