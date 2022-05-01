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
  /*
  it should "run" in {
    test(new Top("src/test/scala/resources/memory/m16k_1.memory")).withAnnotations(Seq(VerilatorBackendAnnotation)) { c =>
      // c is an instance of Top
      while (!c.io.exit.peek().litToBoolean) {
        c.clock.step(1)
      }
    }
  }
  */

  for (f <- new File("src/test/scala/resources/memory"
      ).listFiles.filter(f => f.isFile && f.getName.endsWith(".memory"
      ))) {
    val p = f.getPath
    it should p in {
      test(new Top(p)) { t =>
        // t is an instance of Top
        while (!t.io.exit.peek().litToBoolean) {
          //t.core.decode.stall_flag.expect(false.B)
          t.clock.step(1)
        }
      }
    }
  }
}
