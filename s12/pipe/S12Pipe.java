package pipe;

import core.Cpu;
import core.TraceSink;
import isa.*;

import static isa.Bits.*;

import java.util.*;

public final class S12Pipe implements Cpu {

  // Architectural state
  private int PC = 0, ACC = 0;
  private final int[] mem = new int[256];
  private boolean halted = false;

  // Pipeline regs
  private final Latches.IF_ID  if_id  = new Latches.IF_ID();
  private final Latches.ID_EX  id_ex  = new Latches.ID_EX();
  private final Latches.EX_MEM ex_mem = new Latches.EX_MEM();
  private final Latches.MEM_WB mem_wb = new Latches.MEM_WB();

  // Stats
  private long cycles=0, retired=0, stallCount=0, fwdEXCount=0, fwdMEMCount=0;
  private final long[] opcodeCounts = new long[16];

  // Config
  private boolean enableForwarding = true;

  // Trace
  private TraceSink sink;
  private final ArrayDeque<String> retireQ = new ArrayDeque<>();

  // Cpu impl
  @Override 
  public void reset(){
    PC=0; ACC=0; halted=false;
    Arrays.fill(mem, 0);
    if_id.clear(); id_ex.clear(); ex_mem.clear(); mem_wb.clear();
    cycles=retired=stallCount=fwdEXCount=fwdMEMCount=0;
    Arrays.fill(opcodeCounts, 0);
    retireQ.clear();
  }

  @Override public void loadMemory(int[] mem256){ System.arraycopy(mem256, 0, mem, 0, 256); }
  @Override public void setState(int pc8, int acc12){ 
    PC = m8(pc8); 
    ACC = m12(acc12);
  }

  @Override public void setForwardingEnabled(boolean en){ enableForwarding = en; }
  @Override public boolean isForwardingEnabled(){ return enableForwarding; }

  @Override public void tick(){
    WB();
    MEM();
    EX();
    boolean stalled = hazardDetectAndStallDecision();
    ID(stalled);
    IF(stalled);
    cycles++;
  }

  @Override public void run(Integer maxCycles){
    while (!halted && (maxCycles == null || cycles < maxCycles)) tick();
  }

  // helpers
  @Override public boolean isHalted(){ return halted; }
  @Override public int getPC(){ return PC; }
  @Override public int getACC(){ return ACC; }
  @Override public int[] getMem(){ return mem; }
  @Override public long getCycles(){ return cycles; }
  @Override public long getRetired(){ return retired; }
  @Override public long getStalls(){ return stallCount; }
  @Override public long getFwdEXtoEX(){ return fwdEXCount; }
  @Override public long getFwdMEMtoEX(){ return fwdMEMCount; }
  @Override public long[] getInstructionMix(){ return opcodeCounts; }

  @Override public void setTraceSink(TraceSink s){ this.sink = s; }
  @Override public List<String> drainRetireTrace(){ var out = new ArrayList<String>(retireQ); retireQ.clear(); return out; }

  // --- Stages ---
  private void IF(boolean stalled){
    if (halted) { if_id.valid=false; return; }
    if (stalled) return;
    if_id.pc    = m8(PC);
    if_id.instr = m12(mem[m8(PC)]);
    if_id.valid = true;
    PC = m8(PC + 1);
  }

  private Ctrl decode(Op op){
    Ctrl c = new Ctrl(); c.op = op;
    switch (op){
      case JMP:
      case JN:
      case JZ:  c.BranchJmp=true; break;
      case LOAD:   c.MemRead=true;  c.RegWrite=true;  c.alu=Alu.PASS; break;
      case STORE:  c.MemWrite=true; c.RegWrite=false; c.alu=Alu.PASS; break;
      case LOADI:  c.UseIndir=true; c.MemRead=true;   c.RegWrite=true;  c.alu=Alu.PASS; break;
      case STOREI: c.UseIndir=true; c.MemWrite=true;  c.RegWrite=false; c.alu=Alu.PASS; break;
      case AND:    c.MemRead=true;  c.RegWrite=true;  c.alu=Alu.AND; break;
      case OR:     c.MemRead=true;  c.RegWrite=true;  c.alu=Alu.OR;  break;
      case ADD:    c.MemRead=true;  c.RegWrite=true;  c.alu=Alu.ADD; break;
      case SUB:    c.MemRead=true;  c.RegWrite=true;  c.alu=Alu.SUB; break;
      case HALT:   c.RegWrite=false; c.alu=Alu.NONE; break;
      case NOP: default: break;
    }
    return c;
  }

  private void ID(boolean stalled){
    if (!if_id.valid) { id_ex.clear(); return; }
    if (stalled){ id_ex.clear(); stallCount++; return; }
    int instr = if_id.instr;
    Op op = Op.fromNibble((instr >> 8) & 0xF);
    id_ex.pc     = if_id.pc;
    id_ex.instr  = instr;
    id_ex.op     = op;
    id_ex.X      = instr & 0xFF;
    id_ex.accVal = ACC;
    id_ex.ctrl   = Ctrl.copyOf(decode(op));
    id_ex.valid  = true;
  }

  private static boolean producesInEX(Ctrl c){ return c != null && c.RegWrite && !c.MemRead; }

  private void EX(){
    if (!id_ex.valid) { ex_mem.clear(); return; }

    int a = id_ex.accVal;

    if (enableForwarding){
      // EX->EX forward: producer writes in EX (ALU/pass)
      if (ex_mem.valid && producesInEX(ex_mem.ctrl)) {
        int cand = ex_mem.aluRes;
        if (cand != a) { a = cand; fwdEXCount++; }
      }
      // MEM/WB->EX forward: producer writes in MEM (loads/mem-ALU)
      else if (mem_wb.valid && mem_wb.ctrl != null && mem_wb.ctrl.RegWrite) {
        int cand = mem_wb.wbData;
        if (cand != a) { a = cand; fwdMEMCount++; }
      }
    }

    boolean redirect=false; int target = m8(id_ex.X);
    if (id_ex.ctrl.BranchJmp){
      switch (id_ex.op){
        case JMP: redirect = true; break;
        case JN:  redirect = isNeg12(a); break;
        case JZ:  redirect = (m12(a) == 0); break;
        default: break;
      }
      if (redirect){
        PC = target;
        if_id.valid = false; // flush IF
        id_ex.valid = false; // bubble ID
      }
    }

    ex_mem.pc        = id_ex.pc;
    ex_mem.op        = id_ex.op;
    ex_mem.memAddr   = id_ex.X;
    ex_mem.aluRes    = m12(a);
    ex_mem.storeData = a;
    ex_mem.ctrl      = Ctrl.copyOf(id_ex.ctrl);
    ex_mem.valid     = true;
    ex_mem.branchTaken  = redirect;
    ex_mem.branchTarget = target;
  }

  private void MEM(){
    if (!ex_mem.valid) { mem_wb.clear(); return; }

    int dataOut = ex_mem.aluRes;             // default pass-through (ACC)
    int base = ex_mem.memAddr & 0xFF;
    int eff  = ex_mem.ctrl.UseIndir ? (mem[base] & 0xFF) : base;

    boolean didWrite = false;
    int storeVal = ex_mem.storeData;

    if (ex_mem.ctrl.MemRead){
      int mval = mem[eff] & 0xFFF;

      // Loads: take memory value directly
      if (ex_mem.op == Op.LOAD || ex_mem.op == Op.LOADI){
        dataOut = mval;
      } else {
        // Arithmetic/logic opcodes: ACC (storeData) op mem[eff]
        switch (ex_mem.op){
          case ADD: dataOut = m12(ex_mem.storeData +  mval); break;
          case SUB: dataOut = m12(ex_mem.storeData -  mval); break;
          case AND: dataOut = m12(ex_mem.storeData &  mval); break;
          case OR:  dataOut = m12(ex_mem.storeData |  mval); break;
          default:  dataOut = ex_mem.aluRes; break;
        }
      }
    }

    if (ex_mem.ctrl.MemWrite){
      mem[eff] = m12(storeVal);
      didWrite = true;
    }

    mem_wb.pc          = ex_mem.pc;
    mem_wb.op          = ex_mem.op;
    mem_wb.wbData      = m12(dataOut);
    mem_wb.ctrl        = Ctrl.copyOf(ex_mem.ctrl);
    mem_wb.valid       = true;

    // enrich WB for trace
    mem_wb.didMemWrite = didWrite;
    mem_wb.effAddr     = eff;
    mem_wb.storeVal    = m12(storeVal);
    mem_wb.branchTaken = ex_mem.branchTaken;
    mem_wb.branchTarget= ex_mem.branchTarget;
  }

  private void WB(){
    if (!mem_wb.valid) return;

    if (mem_wb.ctrl.RegWrite) ACC = m12(mem_wb.wbData);

    if (mem_wb.ctrl.op != Op.NOP){
      retired++;
      int oc = mem_wb.ctrl.op.code & 0xF;
      if (oc >= 0 && oc < 16) opcodeCounts[oc]++;
      String line = formatRetireLine(mem_wb);
      if (sink != null) sink.onRetire(line); else retireQ.add(line);
    }
    if (mem_wb.ctrl.op == Op.HALT) halted = true;
  }

  private static boolean isTrueLoad(Ctrl c){ return c != null && c.MemRead && (c.op == Op.LOAD || c.op == Op.LOADI); }
  private static Op decodeIfID(int instr){ return Op.fromNibble((instr >> 8) & 0xF); }

  // --- Hazards ---
  private static boolean needsACC(Op op){
    switch (op){
      case ADD: case SUB: case AND: case OR:
      case STORE: case STOREI:
      case JN: case JZ:
        return true;
      default: return false; // LOAD, LOADI, JMP, HALT, NOP
    }
  }

  private boolean hazardDetectAndStallDecision(){
    //load→use: producer currently in EX and is a true LOAD
    boolean producerValid = ex_mem.valid;
    Ctrl pc = ex_mem.ctrl;
    boolean loadUseProducer = producerValid && isTrueLoad(pc);

    Op idOp = Op.NOP;
    if (if_id.valid) idOp = decodeIfID(if_id.instr);

    boolean consumerNeedsACC = needsACC(idOp);
    return loadUseProducer && consumerNeedsACC;
  }

  // Trace formatting
  private static String hex2(int x){ return String.format("%02X", x & 0xFF); }
  private static String hex3(int x){ return String.format("%03X", x & 0xFFF); }

  private String mnemonicFor(Op op, int instr){
    int X = instr & 0xFF;
    switch (op){
      case JMP:    return "JMP "   + hex2(X);
      case JN:     return "JN "    + hex2(X);
      case JZ:     return "JZ "    + hex2(X);
      case LOAD:   return "LOAD "  + hex2(X);
      case STORE:  return "STORE " + hex2(X);
      case LOADI:  return "LOADI ("+hex2(X)+")";
      case STOREI: return "STOREI ("+hex2(X)+")";
      case AND:    return "AND "   + hex2(X);
      case OR:     return "OR "    + hex2(X);
      case ADD:    return "ADD "   + hex2(X);
      case SUB:    return "SUB "   + hex2(X);
      case HALT:   return "HALT";
      default:     return "NOP";
    }
  }

  private String formatRetireLine(Latches.MEM_WB wb){
    int pc    = wb.pc & 0xFF;
    int instr = mem[pc] & 0xFFF;
    String m  = mnemonicFor(wb.op, instr);

    if (wb.ctrl.BranchJmp){
      String tag = wb.branchTaken ? ("TAKEN -> " + hex2(wb.branchTarget)) : "not taken";
      return String.format("%02X: %-12s | %s", pc, m, tag);
    }
    if (wb.ctrl.MemWrite && wb.didMemWrite){
      return String.format("%02X: %-12s | MEM[%s]<=%s", pc, m, hex2(wb.effAddr), hex3(wb.storeVal));
    }
    if (wb.ctrl.RegWrite){
      return String.format("%02X: %-12s | ACC<=%s", pc, m, hex3(wb.wbData));
    }
    return String.format("%02X: %s", pc, m);
  }
}
