package cpu

import java.io.File
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.BundleLiterals._
import Consts._

class CPUStructTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "mycpu"


  for (f <- new File("src/test/scala/resources/memory").listFiles.filter(f => f.isFile && f.getName.endsWith(".memory"))) {
    val p = f.getPath
    it should p in {
      test(new Top(p)) { c =>
        // c is an instance of Top
        while (!c.io.exit.peek().litToBoolean) {
          c.clock.step(1)
        }
      }
    }
  }
}