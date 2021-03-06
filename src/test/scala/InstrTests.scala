package cpu

import java.io.File
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.BundleLiterals._
import scala.io.StdIn.readLine

class InstrTests extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "mycpu instruction asm test"

  for (f <- new File("./instruction-tests").listFiles.filter(f => f.isFile && f.getName.endsWith(".hex"))) {
    val p = f.getPath
    var cycle = 0
    it should s"pass ${p}" in {
      test(new Top(p)) { c =>
        while (c.io.exit.peek().litToBoolean == false) {
          c.clock.step(1)
          cycle += 1
        }
        // instruction tests sets 1 to gp when the test passed otherwise gp represents which test case failed
        c.io.gp.expect(1.U)
      }
    }
  }
}
