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

// IF
class FetchStage {
  // IF/ID pipeline registers
  val reg_pc = RegInit(0.U(WORD_LEN.W))
  // default instruction is NOP
  val reg_inst = RegInit(BUBBLE)

  def connect(imem: ImemPortIo, decode: DecodeStage, exe: ExecuteStage, csr: Mem[UInt]) = {
    // Connect program counter to address output. This output is connected to memory to fetch the
    // address as instruction
    val pc = RegInit(START_ADDR)

    // Program counter register. It counts up per 4 bytes since size of instruction is 32bits, but
    // memory address is byte oriented.

    val pc_this_cycle = Wire(UInt(WORD_LEN.W))
    val pc_next_cycle = Wire(UInt(WORD_LEN.W))
    val inst = Wire(UInt(WORD_LEN.W))
    pc_this_cycle := MuxCase(pc, Seq(
      exe.br_flag  -> exe.br_target, // Branch instructions write back br_target address to program counter
      exe.jmp_flag -> exe.alu_out, // Jump instructions calculate the jump address by ALU
      // CSRs[0x305] is mtvec (trap_vector). The process on exception (syscall) is written at the
      // trap_vector address. Note that there is no OS on this CPU.
      //(inst === ECALL) -> csr(0x305),
    ))
    pc_next_cycle := Mux(decode.stall_flag, pc_this_cycle, pc_this_cycle + INST_BYTES)

    // Save to IF/ID registers
    // set to current pc address in Fetch stage
    // when data_hazard, Decode stage will exec NOP
    reg_pc := pc_this_cycle
    pc     := pc_next_cycle

    inst      := imem.inst
    imem.addr := pc_this_cycle
    reg_inst  := inst

    printf(p"IF: pc=0x${Hexadecimal(pc)} inst=0x${Hexadecimal(inst)}\n")

    // On branch hazard:
    //
    // Jump instructions cause pipeline branch hazard. Replace the instruction being fetched with NOP
    // not to execute it.
    //
    //      IF     ID     EX     MEM
    //   ┌──────┬──────┐
    //   │INST A│ JUMP │                CYCLE = C
    //   │ (X+4)│ (X)  │
    //   └──────┴──────┘
    //   ┌──────┬──────┬──────┐
    //   │INST B│INST A│JUMP X│         CYCLE = C+1
    //   │ (X+8)│ (X+4)│(X->Y)│
    //   └──┬───┴──┬───┴──────┘
    //      ▼      ▼
    // Reload instuction at Y
    //   ┌──────┬──────┬──────┐
    //   │INST P│ NOP  │JUMP X│         CYCLE = C+1
    //   │ (Y)  │      │(X->Y)│
    //   └──────┴──────┴──────┘
    //   ┌──────┬──────┬──────┬──────┐
    //   │INST Q│INST P│ NOP  │JUMP X│  CYCLE = C+2
    //   │ (Y+4)│ (Y)  │      │(X->Y)│
    //   └──────┴──────┴──────┴──────┘
    //
    //
    // On data hazard:
    // Register data hazard
    // In IF stage, to fetch the register data which is now being calculated at EX stage, waiting in MEM stage and
    // writing back in WB stage. Try the best to forward data from EX, MEM and WB stage.
    //  The data will be forwarded to IF
    // and ID at MEM.
    //
    // When RAW (Read After Write) happens, data will be forwarded
    // Normally, the data to be writed is stored is computed at EX stage in ALU.
    //
    //      IF     ID     EX     MEM    WB
    //   ┌──────┬──────┬──────┬──────┬──────┐
    //   │INST E│INST D│INST C│INST B│INST A│  CYCLE = C
    //   │(X+16)│(X+12)│ (X+8)│ (X+4)│ (X)  │
    //   └──────┴──▲───┴──▼───┴──▼───┴──▼───┘
    //             └──────┴──────┴──────┘
    //                   forward
    //
    //   ┌──────┬──────┬──────┬──────┬──────┐
    //   │INST F│INST E│INST D│INST C│INST B│  CYCLE = C+1
    //   │(X+20)│(X+16)│(X+12)│ (X+8)│ (X+4)│
    //   └──────┴──────┴──────┴──────┴──────┘
    //
    // However, sometimes, data can not be computed at EX stage.
    // For example, read register after LOAD:
    //   load x1 <-- 0x123456
    //   add  x3 <-- x2, x1
    //
    //      IF     ID     EX     MEM    WB
    //   ┌──────┬──────┬──────┬──────┬──────┐
    //   │INST E│INST D│ LOAD │INST B│INST A│  CYCLE = C
    //   │(X+16)│(X+12)│ (X+8)│ (X+4)│ (X)  │
    //   └─┬────┴─┬┬───┴───┬──┴──────┴──────┘
    //     │      │└───┬───┘
    //     │      │ harzard
    //     │      │
    //     │keep  │keep
    //     │      │
    //   ┌─▼────┬─▼────┬──────┬──────┬──────┐
    //   │INST E│INST D│ NOP  │ LOAD │INST B│  CYCLE = C+1
    //   │(X+16)│(X+12)│      │ (X+8)│ (X+4)│
    //   └──────┴───▲──┴──────┴───┬──┴──────┘
    //              └─────────────┘
    //                  forward
    //
  }
}

// ID
class DecodeStage {
  val inst       = Wire(UInt(WORD_LEN.W))
  val stall_flag = Wire(Bool())
  // ID/EX pipeline registers
  val reg_pc            = RegInit(0.U(WORD_LEN.W))
  val reg_wb_addr       = RegInit(0.U(ADDR_LEN.W))
  val reg_op1_data      = RegInit(0.U(WORD_LEN.W))
  val reg_op2_data      = RegInit(0.U(WORD_LEN.W))
  val reg_rs1_data      = RegInit(0.U(WORD_LEN.W))
  val reg_rs2_data      = RegInit(0.U(WORD_LEN.W))
  val reg_exe_fun       = RegInit(0.U(EXE_FUN_LEN.W))
  val reg_mem_wen       = RegInit(0.U(MEN_LEN.W))
  val reg_rf_wen        = RegInit(0.U(REN_LEN.W)) // regfile write enable
  val reg_wb_sel        = RegInit(0.U(WB_SEL_LEN.W))
  val reg_csr_addr      = RegInit(0.U(CSR_ADDR_LEN.W))
  val reg_csr_cmd       = RegInit(0.U(CSR_LEN.W))
  val reg_csr_old_data  = RegInit(0.U(WORD_LEN.W))
  val reg_byte_sel      = RegInit(0.U(WORD_LEN.W))
  val reg_load_flag     = RegInit(false.B)
  val reg_load_unsigned = RegInit(false.B)
  val reg_imm_i_sext    = RegInit(0.U(WORD_LEN.W))
  val reg_imm_s_sext    = RegInit(0.U(WORD_LEN.W))
  val reg_imm_b_sext    = RegInit(0.U(WORD_LEN.W))
  val reg_imm_u_shifted = RegInit(0.U(WORD_LEN.W))
  val reg_imm_z_uext    = RegInit(0.U(WORD_LEN.W))

  // gr: general register file
  def connect(prev: FetchStage, exe: ExecuteStage, mem: MemStage, gr: Mem[UInt], csr: Mem[UInt]) = {
    // Spec 2.2 Base Instruction Formats
    //
    //  31      30 29 28 27 26 25 24 23 22 21   20   19 18 17 16 15 14 13 12 11 10 9 8     7   6 5 4 3 2 1 0
    // ┌─────────────────────────┬──────────────────┬──────────────┬────────┬─────────────────┬─────────────┐
    // │         funct7          │       rs2        │     rs1      │ funct3 │       rd        │   opcode    │ R-type
    // ├─────────────────────────┴──────────────────┼──────────────┼────────┼─────────────────┼─────────────┤
    // │                imm[11:0]                   │     rs1      │ funct3 │       rd        │   opcode    │ I-type
    // ├─────────────────────────┬──────────────────┼──────────────┼────────┼─────────────────┼─────────────┤
    // │        imm[11:5]        │       rs2        │     rs1      │ funct3 │   imm[4:0]      │   opcode    │ S-type
    // ├───────┬─────────────────┼──────────────────┼──────────────┼────────┼─────────┬───────┼─────────────┤
    // │imm[12]│    imm[10:5]    │       rs2        │     rs1      │ funct3 │imm[4:1] │imm[11]│   opcode    │ B-type
    // ├───────┴─────────────────┴──────────────────┴──────────────┴────────┼─────────┴───────┼─────────────┤
    // │                             imm[31:12]                             │       rd        │   opcode    │ U-type
    // ├───────┬────────────────────────────┬───────┬───────────────────────┼─────────────────┼─────────────┤
    // │imm[20]│         imm[10:1]          │imm[11]│      imm[19:12]       │       rd        │   opcode    │ J-type
    // └───────┴────────────────────────────┴───────┴───────────────────────┴─────────────────┴─────────────┘

    // Jump instructions cause pipeline branch hazard. Replace the instruction being fetched with NOP
    // not to execute it.
    // exe.br_flag || exe.jmp_flag is branch hazard
    // prev.data_hazard

    val prev_rs1_addr = prev.reg_inst(19, 15)
    val prev_rs2_addr = prev.reg_inst(24, 20)
    val wb_addr = inst(11, 7) // rd
    // Check rs1, rs2 read after load
    val rs1_RAL = (prev_rs1_addr === reg_wb_addr && reg_rf_wen === REN_SCALAR && reg_load_flag)
    val rs2_RAL = (prev_rs2_addr === reg_wb_addr && reg_rf_wen === REN_SCALAR && reg_load_flag)
    stall_flag := rs1_RAL || rs2_RAL // True when read after load occurs at EX stage, IF read, EX load

    inst := Mux(
      (exe.br_flag || exe.jmp_flag || stall_flag),
      BUBBLE,
      prev.reg_inst
    )
    val rs1_addr = inst(19, 15)
    val rs2_addr = inst(24, 20)

    // Spec 2.6: The effective address is obtained by adding register rs1 to the sign-extended 12-bit offset.
    // sext 12bit value to 32bit value.

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

    // Decode operand sources and memory/register write back behavior
    val List(exe_fun, op1_sel, op2_sel, mem_wen, rf_wen, wb_sel, csr_cmd, byte_sel) = ListLookup(
      inst,
      List(ALU_NONE, OP1_RS1, OP2_RS2, MEN_NONE, REN_NONE, WB_NONE, CSR_NONE, BS_W),
      Array(
        // 2.3 Integer Computational Instructions
        ADDI      -> List(ALU_ADD,  OP1_RS1,  OP2_IMI,  MEN_NONE,   REN_SCALAR, WB_ALU,   CSR_NONE, BS_W), // x[rs1] + sext(imm_i)
        SLTI      -> List(ALU_SLT,  OP1_RS1,  OP2_IMI,  MEN_NONE,   REN_SCALAR, WB_ALU,   CSR_NONE, BS_W), // x[rs1] <s imm_i_sext
        SLTIU     -> List(ALU_SLTU, OP1_RS1,  OP2_IMI,  MEN_NONE,   REN_SCALAR, WB_ALU,   CSR_NONE, BS_W), // x[rs1] <u imm_i_sext
        ANDI      -> List(ALU_AND,  OP1_RS1,  OP2_IMI,  MEN_NONE,   REN_SCALAR, WB_ALU,   CSR_NONE, BS_W), // x[rs1] & sext(imm_i)
        ORI       -> List(ALU_OR ,  OP1_RS1,  OP2_IMI,  MEN_NONE,   REN_SCALAR, WB_ALU,   CSR_NONE, BS_W), // x[rs1] | sext(imm_i)
        XORI      -> List(ALU_XOR,  OP1_RS1,  OP2_IMI,  MEN_NONE,   REN_SCALAR, WB_ALU,   CSR_NONE, BS_W), // x[rs1] ^ sext(imm_i)
        SLLI      -> List(ALU_SLL,  OP1_RS1,  OP2_IMI,  MEN_NONE,   REN_SCALAR, WB_ALU,   CSR_NONE, BS_W), // x[rs1] << imm_i_sext(4,0)
        SRLI      -> List(ALU_SRL,  OP1_RS1,  OP2_IMI,  MEN_NONE,   REN_SCALAR, WB_ALU,   CSR_NONE, BS_W), // x[rs1] >>u imm_i_sext(4,0)
        SRAI      -> List(ALU_SRA,  OP1_RS1,  OP2_IMI,  MEN_NONE,   REN_SCALAR, WB_ALU,   CSR_NONE, BS_W), // x[rs1] >>s imm_i_sext(4,0)
        // 2.4 Integer Register-Register Operations
        ADD       -> List(ALU_ADD,  OP1_RS1,  OP2_RS2,  MEN_NONE,   REN_SCALAR, WB_ALU,   CSR_NONE, BS_W), // x[rs1] + x[rs2]
        SLT       -> List(ALU_SLT,  OP1_RS1,  OP2_RS2,  MEN_NONE,   REN_SCALAR, WB_ALU,   CSR_NONE, BS_W), // x[rs1] <s x[rs2]
        SLTU      -> List(ALU_SLTU, OP1_RS1,  OP2_RS2,  MEN_NONE,   REN_SCALAR, WB_ALU,   CSR_NONE, BS_W), // x[rs1] <u x[rs2]
        AND       -> List(ALU_AND,  OP1_RS1,  OP2_RS2,  MEN_NONE,   REN_SCALAR, WB_ALU,   CSR_NONE, BS_W), // x[rs1] & x[rs2]
        OR        -> List(ALU_OR,   OP1_RS1,  OP2_RS2,  MEN_NONE,   REN_SCALAR, WB_ALU,   CSR_NONE, BS_W), // x[rs1] | x[rs2]
        XOR       -> List(ALU_XOR,  OP1_RS1,  OP2_RS2,  MEN_NONE,   REN_SCALAR, WB_ALU,   CSR_NONE, BS_W), // x[rs1] ^ x[rs2]
        SLL       -> List(ALU_SLL,  OP1_RS1,  OP2_RS2,  MEN_NONE,   REN_SCALAR, WB_ALU,   CSR_NONE, BS_W), // x[rs1] << x[rs2](4,0)
        SRL       -> List(ALU_SRL,  OP1_RS1,  OP2_RS2,  MEN_NONE,   REN_SCALAR, WB_ALU,   CSR_NONE, BS_W), // x[rs1] >>u x[rs2](4,0)
        SUB       -> List(ALU_SUB,  OP1_RS1,  OP2_RS2,  MEN_NONE,   REN_SCALAR, WB_ALU,   CSR_NONE, BS_W), // x[rs1] - x[rs2]
        SRA       -> List(ALU_SRA,  OP1_RS1,  OP2_RS2,  MEN_NONE,   REN_SCALAR, WB_ALU,   CSR_NONE, BS_W), // x[rs1] >>s x[rs2](4,0)
        LUI       -> List(ALU_ADD,  OP1_NONE, OP2_IMU,  MEN_NONE,   REN_SCALAR, WB_ALU,   CSR_NONE, BS_W), // sext(imm_u[31:12] << 12)
        AUIPC     -> List(ALU_ADD,  OP1_PC,   OP2_IMU,  MEN_NONE,   REN_SCALAR, WB_ALU,   CSR_NONE, BS_W), // PC + sext(imm_u[31:12] << 12)
        // 2.5 Control Transfer Instructions
        BEQ       -> List(BR_BEQ,   OP1_RS1,  OP2_RS2,  MEN_NONE,   REN_NONE,   WB_NONE,  CSR_NONE, BS_W), // x[rs1] === x[rs2] then PC+sext(imm_b)
        BNE       -> List(BR_BNE,   OP1_RS1,  OP2_RS2,  MEN_NONE,   REN_NONE,   WB_NONE,  CSR_NONE, BS_W), // x[rs1] =/= x[rs2] then PC+sext(imm_b)
        BGE       -> List(BR_BGE,   OP1_RS1,  OP2_RS2,  MEN_NONE,   REN_NONE,   WB_NONE,  CSR_NONE, BS_W), // x[rs1] >=s x[rs2] then PC+sext(imm_b)
        BGEU      -> List(BR_BGEU,  OP1_RS1,  OP2_RS2,  MEN_NONE,   REN_NONE,   WB_NONE,  CSR_NONE, BS_W), // x[rs1] >=u x[rs2] then PC+sext(imm_b)
        BLT       -> List(BR_BLT,   OP1_RS1,  OP2_RS2,  MEN_NONE,   REN_NONE,   WB_NONE,  CSR_NONE, BS_W), // x[rs1] <s x[rs2]  then PC+sext(imm_b)
        BLTU      -> List(BR_BLTU,  OP1_RS1,  OP2_RS2,  MEN_NONE,   REN_NONE,   WB_NONE,  CSR_NONE, BS_W), // x[rs1] <u x[rs2]  then PC+sext(imm_b)
        JAL       -> List(ALU_ADD,  OP1_PC,   OP2_IMJ,  MEN_NONE,   REN_SCALAR, WB_PC,    CSR_NONE, BS_W), // x[rd] <- PC+4 and PC+sext(imm_j)
        JALR      -> List(ALU_JALR, OP1_RS1,  OP2_IMI,  MEN_NONE,   REN_SCALAR, WB_PC,    CSR_NONE, BS_W), // x[rd] <- PC+4 and (x[rs1]+sext(imm_i))&~1
        // 2.6 Load and Store Instructions
        LB        -> List(ALU_ADD,  OP1_RS1,  OP2_IMI,  MEN_NONE,   REN_SCALAR, WB_MEM,   CSR_NONE, BS_B), // x[rs1] + sext(imm_i)
        LH        -> List(ALU_ADD,  OP1_RS1,  OP2_IMI,  MEN_NONE,   REN_SCALAR, WB_MEM,   CSR_NONE, BS_H), // x[rs1] + sext(imm_i)
        LW        -> List(ALU_ADD,  OP1_RS1,  OP2_IMI,  MEN_NONE,   REN_SCALAR, WB_MEM,   CSR_NONE, BS_W), // x[rs1] + sext(imm_i)
        LBU       -> List(ALU_ADD,  OP1_RS1,  OP2_IMI,  MEN_NONE,   REN_SCALAR, WB_MEM,   CSR_NONE, BS_B), // x[rs1] + sext(imm_i)
        LHU       -> List(ALU_ADD,  OP1_RS1,  OP2_IMI,  MEN_NONE,   REN_SCALAR, WB_MEM,   CSR_NONE, BS_H), // x[rs1] + sext(imm_i)
        SB        -> List(ALU_ADD,  OP1_RS1,  OP2_IMS,  MEN_SCALAR, REN_NONE,   WB_NONE,  CSR_NONE, BS_B), // x[rs1] + sext(imm_s)
        SH        -> List(ALU_ADD,  OP1_RS1,  OP2_IMS,  MEN_SCALAR, REN_NONE,   WB_NONE,  CSR_NONE, BS_H), // x[rs1] + sext(imm_s)
        SW        -> List(ALU_ADD,  OP1_RS1,  OP2_IMS,  MEN_SCALAR, REN_NONE,   WB_NONE,  CSR_NONE, BS_W), // x[rs1] + sext(imm_s)
        // 2.7 Memory Ordering Instructions
        // Currently, no Out-of-Order Instructions, FENCE FENCE.TSO no effect at regfile, mem and order
        FENCE     -> List(ALU_NONE, OP1_NONE, OP2_NONE, MEN_NONE,   REN_NONE,   WB_NONE,  CSR_NONE, BS_W),
        FENCE_TSO -> List(ALU_NONE, OP1_NONE, OP2_NONE, MEN_NONE,   REN_NONE,   WB_NONE,  CSR_NONE, BS_W),

        // 2.8 Environment Call and Breakpoints
        ECALL     -> List(ALU_NONE, OP1_PC,   OP2_NONE, MEN_NONE,   REN_NONE,   WB_CSR,   CSR_ECALL,BS_W),
        // Currently, EBREAK is decoded as NOP
        EBREAK    -> List(ALU_NONE, OP1_NONE, OP2_NONE, MEN_NONE,   REN_NONE,   WB_NONE,  CSR_NONE, BS_W),
        MRET      -> List(ALU_NONE, OP1_NONE, OP2_NONE, MEN_NONE,   REN_NONE,   WB_NONE,  CSR_NONE, BS_W),

        // 9.1 "Zicsr", Control and Status Register (CSR) Instructions
        // "Zicsr" is I-type
        CSRRW     -> List(ALU_RS1,  OP1_RS1,  OP2_NONE, MEN_NONE,   REN_SCALAR, WB_CSR,   CSR_W,    BS_W), // CSRs[csr] <==> x[rs1]
        CSRRWI    -> List(ALU_RS1,  OP1_IMZ,  OP2_NONE, MEN_NONE,   REN_SCALAR, WB_CSR,   CSR_W,    BS_W), // CSRs[csr] <==> uext(imm_z)
        CSRRS     -> List(ALU_RS1,  OP1_RS1,  OP2_NONE, MEN_NONE,   REN_SCALAR, WB_CSR,   CSR_S,    BS_W), // CSRs[csr] <- CSRs[csr] | x[rs1]
        CSRRSI    -> List(ALU_RS1,  OP1_IMZ,  OP2_NONE, MEN_NONE,   REN_SCALAR, WB_CSR,   CSR_S,    BS_W), // CSRs[csr] <- CSRs[csr] | uext(imm_z)
        CSRRC     -> List(ALU_RS1,  OP1_RS1,  OP2_NONE, MEN_NONE,   REN_SCALAR, WB_CSR,   CSR_C,    BS_W), // CSRs[csr] <- CSRs[csr]&~x[rs1]
        CSRRCI    -> List(ALU_RS1,  OP1_IMZ,  OP2_NONE, MEN_NONE,   REN_SCALAR, WB_CSR,   CSR_C,    BS_W), // CSRs[csr] <- CSRs[csr]&~uext(imm_z)
      ),
    )

    // MuxCase priority from first to last
    val rs1_data = MuxCase(gr(rs1_addr), Seq(
      // The value of register #0 is always zero
      (rs1_addr === 0.U) -> 0.U(WORD_LEN.W),
      // Forward data from EX, MEM and WB stage
      // Forward from EX stage
      (rs1_addr === reg_wb_addr && reg_rf_wen === REN_SCALAR) -> exe.alu_out,
      // Forward data from MEM stage to avoid data hazard. The same as above.
      (rs1_addr === exe.reg_wb_addr && exe.reg_rf_wen === REN_SCALAR) -> exe.reg_alu_out,
      // Forward data from WB stage to avoid data hazard. The same as above.
      (rs1_addr === mem.reg_wb_addr && mem.reg_rf_wen === REN_SCALAR) -> mem.reg_wb_data
    ))
    val rs2_data = MuxCase(gr(rs2_addr), Seq(
      // The value of register #0 is always zero
      (rs2_addr === 0.U) -> 0.U(WORD_LEN.W),
      // Forward data from EX, MEM and WB stage
      // Forward from EX stage
      (rs2_addr === reg_wb_addr && reg_rf_wen === REN_SCALAR) -> exe.alu_out,
      // Forward data from MEM stage to avoid data hazard. The same as above.
      (rs2_addr === exe.reg_wb_addr && exe.reg_rf_wen === REN_SCALAR) -> exe.reg_alu_out,
      // Forward data from WB stage to avoid data hazard. The same as above.
      (rs2_addr === mem.reg_wb_addr && mem.reg_rf_wen === REN_SCALAR) -> mem.reg_wb_data
    ))

    // Decode CSR instructions
    val csr_addr = Mux(
      csr_cmd === CSR_ECALL,
      // Two operations: set csr:mepc = pc and csr:mcause = 11 (M ecall)
      CSR_MEPC,
      inst(31, 20), // I-type imm value
    )
    // Handle CSR instructions
    val csr_old_data = MuxCase(csr(csr_addr), /* Read CSRs[csr] */ Seq(
      // Forward data from EX, MEM and WB stage
      // Forward from EX stage， new csr value is new_csr
      (csr_addr === reg_csr_addr && reg_csr_cmd === WB_CSR) -> exe.new_csr,
      // Forward data from MEM stage to avoid data hazard. The same as above.
      (csr_addr === exe.reg_csr_addr && exe.reg_csr_cmd === WB_CSR) -> exe.reg_new_csr,
      // Forward data from WB stage to avoid data hazard. The same as above.
      (rs2_addr === mem.reg_csr_addr && mem.reg_csr_cmd === WB_CSR) -> mem.reg_new_csr
    ))

    val load_flag = MuxCase(false.B, Seq(
      (inst === LB)   -> true.B,
      (inst === LH)   -> true.B,
      (inst === LW)   -> true.B,
      (inst === LBU)  -> true.B,
      (inst === LHU)  -> true.B,
    ))
    val load_unsigned = (inst === LBU || inst === LHU)

    // Determine 1st operand data signal
    val op1_data = MuxCase(0.U(WORD_LEN.W), Seq(
      (op1_sel === OP1_RS1) -> rs1_data,
      (op1_sel === OP1_PC)  -> prev.reg_pc,
      (op1_sel === OP1_IMZ) -> imm_z_uext,
    ))

    // Determine 2nd operand data signal
    val op2_data = MuxCase(0.U(WORD_LEN.W), Seq(
      (op2_sel === OP2_RS2) -> rs2_data,
      (op2_sel === OP2_IMI) -> imm_i_sext,
      (op2_sel === OP2_IMS) -> imm_s_sext,
      (op2_sel === OP2_IMJ) -> imm_j_sext,
      (op2_sel === OP2_IMU) -> imm_u_shifted, // for LUI and AUIPC
    ))

    // Save ID states for next stage
    reg_pc            := prev.reg_pc
    reg_op1_data      := op1_data
    reg_op2_data      := op2_data
    reg_rs1_data      := rs1_data
    reg_rs2_data      := rs2_data
    reg_wb_addr       := wb_addr
    reg_rf_wen        := rf_wen
    reg_exe_fun       := exe_fun
    reg_wb_sel        := wb_sel
    reg_imm_i_sext    := imm_i_sext
    reg_imm_s_sext    := imm_s_sext
    reg_imm_b_sext    := imm_b_sext
    reg_imm_u_shifted := imm_u_shifted
    reg_csr_addr      := csr_addr
    reg_csr_cmd       := csr_cmd
    reg_csr_old_data  := csr_old_data
    reg_mem_wen       := mem_wen
    reg_byte_sel      := byte_sel
    reg_load_flag     := load_flag
    reg_load_unsigned := load_unsigned


    // DEBUG forward
    val rs1_hazard = (rs1_addr === reg_wb_addr && reg_rf_wen === REN_SCALAR && rs1_addr =/= 0.U) ||
      (rs1_addr === exe.reg_wb_addr && exe.reg_rf_wen === REN_SCALAR && rs1_addr =/= 0.U) ||
      (rs1_addr === mem.reg_wb_addr && mem.reg_rf_wen === REN_SCALAR && rs1_addr =/= 0.U)
    val rs2_hazard = (rs2_addr === reg_wb_addr && reg_rf_wen === REN_SCALAR && rs1_addr =/= 0.U) ||
      (rs2_addr === exe.reg_wb_addr && exe.reg_rf_wen === REN_SCALAR && rs1_addr =/= 0.U) ||
      (rs2_addr === mem.reg_wb_addr && mem.reg_rf_wen === REN_SCALAR && rs1_addr =/= 0.U)
    printf(p"[Debug] ID: rs1_addr=${rs1_addr} reg_wb_addr=${reg_wb_addr}\n")
    printf(p"""ID: pc=0x${Hexadecimal(prev.reg_pc)} asm ${DbgDecoder.decode(inst, prev.reg_pc)}
      inst=0x${Hexadecimal(inst)}
          rs1=${rs1_data} rs2=${rs2_data}
      data_hazard=${rs1_hazard || rs2_hazard}
          rs1_hazard=${rs1_hazard} rs2_hazard=${rs2_hazard}\n""")
  }
}

// EX
class ExecuteStage {
  val br_flag = Wire(Bool())
  val br_target = Wire(UInt(WORD_LEN.W))
  val jmp_flag = Wire(Bool())
  val alu_out = Wire(UInt(WORD_LEN.W))
  val new_csr = Wire(UInt(WORD_LEN.W))

  // EX/MEM pipeline registers
  val reg_pc            = RegInit(0.U(WORD_LEN.W))
  val reg_wb_addr       = RegInit(0.U(ADDR_LEN.W))
  val reg_op1_data      = RegInit(0.U(WORD_LEN.W))
  val reg_rs1_data      = RegInit(0.U(WORD_LEN.W))
  val reg_rs2_data      = RegInit(0.U(WORD_LEN.W))
  val reg_mem_wen       = RegInit(0.U(MEN_LEN.W))
  val reg_rf_wen        = RegInit(0.U(REN_LEN.W))
  val reg_wb_sel        = RegInit(0.U(WB_SEL_LEN.W))
  val reg_csr_addr      = RegInit(0.U(CSR_ADDR_LEN.W))
  val reg_csr_cmd       = RegInit(0.U(CSR_LEN.W))
  val reg_new_csr       = RegInit(0.U(WORD_LEN.W))
  val reg_imm_i_sext    = RegInit(0.U(WORD_LEN.W))
  val reg_imm_z_uext    = RegInit(0.U(WORD_LEN.W))
  val reg_alu_out       = RegInit(0.U(WORD_LEN.W))
  val reg_byte_sel      = RegInit(0.U(BS_LEN))
  val reg_load_unsigned = RegInit(false.B)

  def connect(prev: DecodeStage) = {
    // Arithmetic Logic Unit process arithmetic/logical calculations for each instruction.
    alu_out := MuxCase(0.U(WORD_LEN.W), Seq(
      (prev.reg_exe_fun === ALU_ADD)  -> (prev.reg_op1_data + prev.reg_op2_data),
      (prev.reg_exe_fun === ALU_SUB)  -> (prev.reg_op1_data - prev.reg_op2_data),
      (prev.reg_exe_fun === ALU_AND)  -> (prev.reg_op1_data & prev.reg_op2_data),
      (prev.reg_exe_fun === ALU_OR)   -> (prev.reg_op1_data | prev.reg_op2_data),
      (prev.reg_exe_fun === ALU_XOR)  -> (prev.reg_op1_data ^ prev.reg_op2_data),
      // Note: (31, 0) is necessary because << extends bits of the result value
      // Note: (4, 0) is necessary for I instructions (imm[4:0])
      (prev.reg_exe_fun === ALU_SLL)  -> (prev.reg_op1_data << prev.reg_op2_data(4, 0))(31, 0),
      (prev.reg_exe_fun === ALU_SRL)  -> (prev.reg_op1_data >> prev.reg_op2_data(4, 0)).asUInt(),
      (prev.reg_exe_fun === ALU_SRA)  -> (prev.reg_op1_data.asSInt() >> prev.reg_op2_data(4, 0)).asUInt(),
      // Compare as signed integers
      (prev.reg_exe_fun === ALU_SLT)  -> (prev.reg_op1_data.asSInt() < prev.reg_op2_data.asSInt()).asUInt(),
      (prev.reg_exe_fun === ALU_SLTU) -> (prev.reg_op1_data < prev.reg_op2_data).asUInt(),
      // &~1 sets the LSB to zero (& 0b1111..1110) for jump instructions
      (prev.reg_exe_fun === ALU_JALR) -> ((prev.reg_op1_data + prev.reg_op2_data) & ~1.U(WORD_LEN.W)),
      (prev.reg_exe_fun === ALU_RS1)  -> prev.reg_op1_data,
    ))

    new_csr := MuxCase(0.U(WORD_LEN.W), Seq(
      (prev.reg_csr_cmd === CSR_W)     -> prev.reg_op1_data, // Write
      (prev.reg_csr_cmd === CSR_S)     -> (prev.reg_csr_old_data | prev.reg_op1_data), // Read and Set Bits
      (prev.reg_csr_cmd === CSR_C)     -> (prev.reg_csr_old_data & ~prev.reg_op1_data), // Read and Clear Bits
      (prev.reg_csr_cmd === CSR_ECALL) -> prev.reg_pc // store pc to epc
    ))

    // Branch instructions
    br_flag := MuxCase(false.B, Seq(
      (prev.reg_exe_fun === BR_BEQ)  ->  (prev.reg_op1_data === prev.reg_op2_data),
      (prev.reg_exe_fun === BR_BNE)  -> !(prev.reg_op1_data === prev.reg_op2_data),
      (prev.reg_exe_fun === BR_BLT)  ->  (prev.reg_op1_data.asSInt() < prev.reg_op2_data.asSInt()),
      (prev.reg_exe_fun === BR_BGE)  -> !(prev.reg_op1_data.asSInt() < prev.reg_op2_data.asSInt()),
      (prev.reg_exe_fun === BR_BLTU) ->  (prev.reg_op1_data < prev.reg_op2_data),
      (prev.reg_exe_fun === BR_BGEU) -> !(prev.reg_op1_data < prev.reg_op2_data),
    ))
    br_target := prev.reg_pc + prev.reg_imm_b_sext

    // only JAL and JALR enables WB_PC signal in reg_wb_sel
    jmp_flag := prev.reg_wb_sel === WB_PC

    // Save EX states for next stage
    reg_pc            := prev.reg_pc
    reg_op1_data      := prev.reg_op1_data
    reg_rs1_data      := prev.reg_rs1_data
    reg_rs2_data      := prev.reg_rs2_data
    reg_wb_addr       := prev.reg_wb_addr
    reg_alu_out       := alu_out
    reg_rf_wen        := prev.reg_rf_wen
    reg_wb_sel        := prev.reg_wb_sel
    reg_csr_addr      := prev.reg_csr_addr
    reg_csr_cmd       := prev.reg_csr_cmd
    reg_new_csr       := new_csr
    reg_imm_i_sext    := prev.reg_imm_i_sext
    reg_imm_z_uext    := prev.reg_imm_z_uext
    reg_mem_wen       := prev.reg_mem_wen
    reg_byte_sel      := prev.reg_byte_sel
    reg_load_unsigned := prev.reg_load_unsigned

    printf(p"EX: pc=0x${Hexadecimal(prev.reg_pc)} wb_addr=${prev.reg_wb_addr} op1=0x${Hexadecimal(prev.reg_op1_data)} op2=0x${Hexadecimal(prev.reg_op2_data)} alu_out=0x${Hexadecimal(alu_out)} jmp=${jmp_flag}\n")
  }
}

// MEM
class MemStage {
  val wb_data = Wire(UInt(WORD_LEN.W)) // Declare wire for forwarding (p.200)
  // MEM/WB pipeline registers
  val reg_wb_addr  = RegInit(0.U(ADDR_LEN.W))
  val reg_rf_wen   = RegInit(0.U(REN_LEN.W))
  val reg_wb_data  = RegInit(0.U(WORD_LEN.W))
  val reg_csr_addr = RegInit(0.U(WORD_LEN.W))
  val reg_csr_cmd  = RegInit(0.U(WORD_LEN.W))
  val reg_new_csr  = RegInit(0.U(WORD_LEN.W))
  val reg_pc       = RegInit(0.U(WORD_LEN.W))

  def connect(dmem: DmemPortIo, prev: ExecuteStage, decode: DecodeStage) = {
    dmem.addr := prev.reg_alu_out // Always output data to memory regardless of instruction
    dmem.wen := prev.reg_mem_wen // mem_wen is integer and here it is implicitly converted to bool
    dmem.wdata := MuxCase(prev.reg_rs2_data, Seq(
      (prev.reg_byte_sel === BS_B) -> (
        dmem.rdata | prev.reg_rs2_data(7,0)  // load byte unsigned
      ),
      (prev.reg_byte_sel === BS_H) -> (
        dmem.rdata | prev.reg_rs2_data(15,0) // load half word unsigned
      )
    ))

    // By default, write back the ALU result to register (wb_sel == WB_ALU)
    val mem_read_ext = MuxCase(dmem.rdata, Seq(
      (prev.reg_byte_sel === BS_B) -> Mux(prev.reg_load_unsigned,
        dmem.rdata & 0x000000ff.U,                        // load byte unsigned
        Cat(Fill(24, dmem.rdata(7)),  dmem.rdata(7,0))    // load byte signed
      ),
      (prev.reg_byte_sel === BS_H) -> Mux(prev.reg_load_unsigned,
        dmem.rdata & 0x0000ffff.U,                        // load half word unsigned
        Cat(Fill(16, dmem.rdata(15)), dmem.rdata(15,0))   // load half word signed
      )
    ))

    wb_data := MuxCase(prev.reg_alu_out, Seq(
      (prev.reg_wb_sel === WB_MEM) -> mem_read_ext, // Loaded data from memory
      // Jump instruction stores the next pc (pc+4) to x[rd]
      (prev.reg_wb_sel === WB_PC) -> (prev.reg_pc + 4.U(WORD_LEN.W))
    ))


    // Save MEM states for next stage
    reg_wb_addr  := prev.reg_wb_addr
    reg_rf_wen   := prev.reg_rf_wen
    reg_wb_data  := wb_data
    // save csr writeback
    reg_csr_addr := prev.reg_csr_addr
    reg_csr_cmd  := prev.reg_csr_cmd
    reg_new_csr  := prev.reg_new_csr
    // forward pc
    reg_pc       := prev.reg_pc

    printf(p"MEM: pc=0x${Hexadecimal(prev.reg_pc)} wb_data=0x${Hexadecimal(wb_data)} rs1=0x${Hexadecimal(prev.reg_rs1_data)} rs2=0x${Hexadecimal(prev.reg_rs2_data)}\n")
  }
}

// WB
class WriteBackStage {

  def connect(prev: MemStage, gr: Mem[UInt], vr: Mem[UInt], csr: Mem[UInt]) = {
    when(prev.reg_rf_wen === REN_SCALAR) {
      gr(prev.reg_wb_addr) := prev.reg_wb_data // Write back to the register specified by rd
    }
    when(prev.reg_csr_cmd =/= WB_CSR) {
      csr(prev.reg_csr_addr) := prev.reg_new_csr // Write back CSR[addr]
      when(prev.reg_csr_cmd === CSR_ECALL) {
      // ECALL, update mepc
      csr(CSR_MEPC) := prev.reg_pc
      // updates mcause
      // mcause = 11 means Environment call from M-mode
      csr(CSR_MCAUSE) := 11.U(WORD_LEN.W)
    }
    }

    printf(p"WB: wb_data=0x${Hexadecimal(prev.reg_wb_data)} new_csr=0x${Hexadecimal(prev.reg_new_csr)}\n")
  }
}

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

  val fetch = new FetchStage()
  val decode = new DecodeStage()
  val execute = new ExecuteStage()
  val mem = new MemStage()
  val wb = new WriteBackStage()

  fetch.connect(io.imem, decode, execute, csr_regfile)
  decode.connect(fetch, execute, mem, regfile, csr_regfile)
  execute.connect(decode)
  mem.connect(io.dmem, execute, decode)
  wb.connect(mem, regfile, vec_regfile, csr_regfile)

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
  io.exit := execute.jmp_flag && (decode.reg_pc === execute.alu_out)
  //io.exit := fetch.reg_pc >= 64.U(WORD_LEN.W)

  io.gp := regfile(3)
  io.pc := execute.reg_pc

  printf(p"dmem: addr=${io.dmem.addr} wen=${io.dmem.wen} wdata=0x${Hexadecimal(io.dmem.wdata)}\n") // memory address loaded by LW

  when(io.exit) {
    printf(p"returned from main with ${regfile(10)}\n") // x10 = a0 = return value or function argument 0
  }
  printf("----------------\n")
}
