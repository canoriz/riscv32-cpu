package cpu
import chisel3._
import chisel3.util._
import Consts._
import Instructions._

object DbgDecoder {
  def decode(inst: UInt, pc: UInt) = {
    val rs1_addr = inst(19, 15)
    val rs2_addr = inst(24, 20)
    val rd_addr  = inst(11, 7)

    // imm for I-type
    val imm_i = inst(31, 20)
    val imm_i_sext = Cat(Fill(20, imm_i(11)), imm_i)

    // imm for S-type
    val imm_s = Cat(inst(31, 25), inst(11, 7))
    val imm_s_sext = Cat(Fill(20, imm_s(11)), imm_s)

    // Decode imm of B-type instruction
    val imm_b = Cat(inst(31), inst(7), inst(30, 25), inst(11, 8))
    // imm[0] does not exist in B-type instruction. This is because the first bit of program counter
    // is always zero (p.126). Size of instruction is 32bit or 16bit, so instruction pointer (pc)
    // address always points an even address.
    val imm_b_sext = Cat(Fill(19, imm_b(11)), imm_b, 0.U(1.U))

    // Decode imm of U-type instruction
    val imm_u = inst(31, 12)
    val imm_u_shifted = Cat(imm_u, Fill(12, 0.U)) // for LUI and AUIPC

    // Decode imm of J-type instruction
    val imm_j = Cat(inst(31), inst(19, 12), inst(20), inst(30, 21))
    val imm_j_sext = Cat(Fill(11, imm_j(19)), imm_j, 0.U(1.U)) // Set LSB to zero

    // Decode imm of Zicsr I-type instruction
    val imm_z = inst(19, 15)
    val imm_z_uext = Cat(Fill(27, 0.U), imm_z) // for CSR instructions 5 bit u-imm


    val asm_list = Seq(
      ADDI 			-> p"[addi     x${rd_addr}, x${rs1_addr}, x${imm_i_sext}]",
      SLTI 			-> p"[slti     x${rd_addr}, x${rs1_addr}, x${imm_i_sext}]",
      SLTIU 		-> p"[sltiu    x${rd_addr}, x${rs1_addr}, x${imm_i_sext.asSInt()}]",
      ANDI 			-> p"[andi     x${rd_addr}, x${rs1_addr}, x${imm_i_sext}]",
      ORI 			-> p"[ori      x${rd_addr}, x${rs1_addr}, x${imm_i_sext}]",
      XORI 			-> p"[xori     x${rd_addr}, x${rs1_addr}, x${imm_i_sext}]",
      SLLI 			-> p"[slli     x${rd_addr}, x${rs1_addr}, x${imm_i_sext(4, 0)}]",
      SRLI 			-> p"[srli     x${rd_addr}, x${rs1_addr}, x${imm_i_sext(4, 0)}]",
      SRAI 			-> p"[srai     x${rd_addr}, x${rs1_addr}, x${imm_i_sext(4, 0)}]",

      ADD 			-> p"[add      x${rd_addr}, x${rs1_addr}, x${rs2_addr}]",
      SLT 			-> p"[slt      x${rd_addr}, x${rs1_addr}, x${rs2_addr}]",
      SLTU 			-> p"[sltu     x${rd_addr}, x${rs1_addr}, x${rs2_addr}]",
      AND 			-> p"[and      x${rd_addr}, x${rs1_addr}, x${rs2_addr}]",
      OR 				-> p"[or       x${rd_addr}, x${rs1_addr}, x${rs2_addr}]",
      XOR 			-> p"[xor      x${rd_addr}, x${rs1_addr}, x${rs2_addr}]",
      SLL 			-> p"[sll      x${rd_addr}, x${rs1_addr}, x${rs2_addr}]",
      SRL 			-> p"[srl      x${rd_addr}, x${rs1_addr}, x${rs2_addr}]",
      SUB 			-> p"[sub      x${rd_addr}, x${rs1_addr}, x${rs2_addr}]",
      SRA 			-> p"[sra      x${rd_addr}, x${rs1_addr}, x${rs2_addr}]",

      LUI 			-> p"[lui      x${rd_addr}, ${imm_u_shifted}]",
      AUIPC 		-> p"[auipc    x${rd_addr}, ${imm_u_shifted}]",

      BEQ 			-> p"[beq      x${rs1_addr}, x${rs2_addr}, ${pc + imm_b_sext}]",
      BNE 			-> p"[bne      x${rs1_addr}, x${rs2_addr}, ${pc + imm_b_sext}]",
      BGE 			-> p"[bge      x${rs1_addr}, x${rs2_addr}, ${pc + imm_b_sext}]",
      BGEU 			-> p"[bgeu     x${rs1_addr}, x${rs2_addr}, ${pc + imm_b_sext}]",
      BLT 			-> p"[blt      x${rs1_addr}, x${rs2_addr}, ${pc + imm_b_sext}]",
      BLTU 			-> p"[bltu     x${rs1_addr}, x${rs2_addr}, ${pc + imm_b_sext}]",
      JAL 			-> p"[jal      x${rd_addr}, ${pc + imm_j_sext}]",
      JALR 			-> p"[jalr     x${rd_addr}, ${imm_i_sext}(x${rs1_addr})]",

      LB 		    -> p"[lb       x${rd_addr}, ${imm_i_sext}(x${rs1_addr})]",
      LH 		    -> p"[lh       x${rd_addr}, ${imm_i_sext}(x${rs1_addr})]",
      LW 		    -> p"[lw       x${rd_addr}, ${imm_i_sext}(x${rs1_addr})]",
      LBU 	  	-> p"[lbu      x${rd_addr}, ${imm_i_sext}(x${rs1_addr})]",
      LHU 	  	-> p"[lhu      x${rd_addr}, ${imm_i_sext}(x${rs1_addr})]",
      SB 	    	-> p"[sb       x${rd_addr}, ${imm_s_sext}(x${rs1_addr})]",
      SH 	    	-> p"[sh       x${rd_addr}, ${imm_s_sext}(x${rs1_addr})]",
      SW 	    	-> p"[sw       x${rd_addr}, ${imm_s_sext}(x${rs1_addr})]",

      FENCE 		-> p"[fence    TODO]",
      FENCE_TSO -> p"[fence.tso]",
      ECALL 		-> p"[ecall]",
      EBREAK 		-> p"[ebreak]"
    )

    var asm_text:chisel3.Printable = p"unknown"
    when (inst === ADDI) {
      asm_text = p"[addi     x${rd_addr}, x${rs1_addr}, x${imm_i_sext}]"
    }
    asm_text
  }
}