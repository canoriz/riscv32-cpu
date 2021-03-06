package cpu

import java.io.File
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.BundleLiterals._
import scala.io.StdIn.readLine

class CTests extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "mycpu c test"

  for (f <- new File("./c-tests").listFiles.filter(f => f.isFile && f.getName.endsWith(".hex"))) {
    val p = f.getPath
    var cycle = 0
    it should s"pass ${p}" in {
      test(new Top(p)) { c =>
        c.clock.setTimeout(0)
        while (c.io.exit.peek().litToBoolean == false) {
          c.clock.step(1)
          cycle = cycle + 1
        }
        // instruction tests sets 1 to gp when the test passed otherwise gp represents which test case failed
        c.io.gp.expect(1.U)
        println(s"Final cycles:${cycle}")
      }
    }
  }
}