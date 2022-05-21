package cpu

import chisel3._
import chisel3.util._

object Consts {
  val WORD_LEN      = 32
  val INST_BYTES    = 4.U(WORD_LEN.W)
  val START_ADDR    = 0.U(WORD_LEN.W)
  val BUBBLE        = 0x00000013.U(WORD_LEN.W)  // [ADDI x0,x0,0] = NOP = BUBBLE (2.4)
  val UNIMP         = "x_c0001073".U(WORD_LEN.W) // [CSRRW x0, cycle, x0] p.159
  val ADDR_LEN      = 5 // rs1,rs2,wb

  val CSR_ADDR_LEN  = 12
  // Main trap setup. priv spec p.10
  val CSR_MSTATUS     = 0x300.U(CSR_ADDR_LEN.W)
  val CSR_MISA        = 0x301.U(CSR_ADDR_LEN.W)
  val CSR_MEDELEG     = 0x302.U(CSR_ADDR_LEN.W)
  val CSR_MIDELEG     = 0x303.U(CSR_ADDR_LEN.W)
  val CSR_MIE         = 0x304.U(CSR_ADDR_LEN.W)
  val CSR_MTVEC       = 0x305.U(CSR_ADDR_LEN.W)
  val CSR_MCOUNTEREN  = 0x306.U(CSR_ADDR_LEN.W)
  val CSR_MSTATUSH    = 0x310.U(CSR_ADDR_LEN.W)
  // Main trap handling
  val CSR_MSCRATCH    = 0x340.U(CSR_ADDR_LEN.W)
  val CSR_MEPC        = 0x341.U(CSR_ADDR_LEN.W)
  val CSR_MCAUSE      = 0x342.U(CSR_ADDR_LEN.W)
  val CSR_MTVAL       = 0x343.U(CSR_ADDR_LEN.W)
  val CSR_MIP         = 0x344.U(CSR_ADDR_LEN.W)
  val CSR_MTINST      = 0x34a.U(CSR_ADDR_LEN.W)
  val CSR_MTVAL2      = 0x34b.U(CSR_ADDR_LEN.W)

  val VLEN          = 128
  val LMUL_LEN      = 2
  val SEW_LEN       = 11
  val VL_ADDR       = 0xC20
  val VTYPE_ADDR    = 0xC21

  val EXE_FUN_LEN = 5
  val ALU_NONE    =  0.U(EXE_FUN_LEN.W)
  val ALU_ADD     =  1.U(EXE_FUN_LEN.W)
  val ALU_SUB     =  2.U(EXE_FUN_LEN.W)
  val ALU_AND     =  3.U(EXE_FUN_LEN.W)
  val ALU_OR      =  4.U(EXE_FUN_LEN.W)
  val ALU_XOR     =  5.U(EXE_FUN_LEN.W)
  val ALU_SLL     =  6.U(EXE_FUN_LEN.W)
  val ALU_SRL     =  7.U(EXE_FUN_LEN.W)
  val ALU_SRA     =  8.U(EXE_FUN_LEN.W)
  val ALU_SLT     =  9.U(EXE_FUN_LEN.W)
  val ALU_SLTU    = 10.U(EXE_FUN_LEN.W)
  val BR_BEQ      = 11.U(EXE_FUN_LEN.W)
  val BR_BNE      = 12.U(EXE_FUN_LEN.W)
  val BR_BLT      = 13.U(EXE_FUN_LEN.W)
  val BR_BGE      = 14.U(EXE_FUN_LEN.W)
  val BR_BLTU     = 15.U(EXE_FUN_LEN.W)
  val BR_BGEU     = 16.U(EXE_FUN_LEN.W)
  val ALU_JALR    = 17.U(EXE_FUN_LEN.W)
  val ALU_RS1     = 18.U(EXE_FUN_LEN.W) // Copy RS1
  val ALU_VADDVV  = 19.U(EXE_FUN_LEN.W)
  val VSET        = 20.U(EXE_FUN_LEN.W)
  val ALU_PCNT    = 21.U(EXE_FUN_LEN.W)

  val OP1_LEN  = 3
  val OP1_RS1  = 0.U(OP1_LEN.W)
  val OP1_PC   = 1.U(OP1_LEN.W)
  val OP1_NONE = 2.U(OP1_LEN.W)
  val OP1_IMZ  = 3.U(OP1_LEN.W)
  val OP1_NRS1 = 4.U(OP1_LEN.W)

  val OP2_LEN  = 3
  val OP2_NONE = 0.U(OP2_LEN.W)
  val OP2_RS2  = 1.U(OP2_LEN.W)
  val OP2_IMI  = 2.U(OP2_LEN.W)
  val OP2_IMS  = 3.U(OP2_LEN.W)
  val OP2_IMJ  = 4.U(OP2_LEN.W)
  val OP2_IMU  = 5.U(OP2_LEN.W)
  var OP2_CSR  = 6.U(OP2_LEN.W)

  val MEN_LEN    = 2
  val MEN_NONE   = 0.U(MEN_LEN.W)
  val MEN_SCALAR = 1.U(MEN_LEN.W) // Scalar
  val MEN_VECTOR = 2.U(MEN_LEN.W) // Vector

// Choose byte
  val BS_LEN     = 2
  val BS_B       = 0.U(BS_LEN.W) // Byte
  val BS_H       = 1.U(BS_LEN.W) // Half word
  val BS_W       = 2.U(BS_LEN.W) // Word

  val REN_LEN    = 2
  val REN_NONE   = 0.U(REN_LEN.W)
  val REN_SCALAR = 1.U(REN_LEN.W) // Scalar
  val REN_VECTOR = 2.U(REN_LEN.W) // Vector

  val WB_SEL_LEN = 3
  val WB_NONE    = 0.U(WB_SEL_LEN.W)
  val WB_ALU     = 0.U(WB_SEL_LEN.W)
  val WB_MEM     = 1.U(WB_SEL_LEN.W)
  val WB_PC      = 2.U(WB_SEL_LEN.W)
  val WB_CSR     = 3.U(WB_SEL_LEN.W)
  val WB_MEM_V   = 4.U(WB_SEL_LEN.W)
  val WB_ALU_V   = 5.U(WB_SEL_LEN.W)
  val WB_VL      = 6.U(WB_SEL_LEN.W)

  val MW_LEN = 3
  val MW_X   = 0.U(MW_LEN.W)
  val MW_W   = 1.U(MW_LEN.W)
  val MW_H   = 2.U(MW_LEN.W)
  val MW_B   = 3.U(MW_LEN.W)
  val MW_HU  = 4.U(MW_LEN.W)
  val MW_BU  = 5.U(MW_LEN.W)

  val CSR_LEN   = 3
  val CSR_NONE  = 0.U(CSR_LEN.W)
  val CSR_W     = 1.U(CSR_LEN.W) // Write
  val CSR_S     = 2.U(CSR_LEN.W) // Set bits
  val CSR_C     = 3.U(CSR_LEN.W) // Clear bits
  val CSR_ECALL = 4.U(CSR_LEN.W) // Exception (ECALL)
  val CSR_V     = 5.U(CSR_LEN.W)
}
