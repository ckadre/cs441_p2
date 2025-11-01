package isa;

public enum Op {
  JMP(0x0), JN(0x1), JZ(0x2),
  LOAD(0x4), STORE(0x5),
  LOADI(0x6), STOREI(0x7),
  AND(0x8), OR(0x9),
  ADD(0xA), SUB(0xB),
  HALT(0xF),
  NOP(0x10);

  public final int code;
  Op(int c){ this.code=c; }

  public static Op fromNibble(int nib){
    switch(nib & 0xF){
      case 0x0: return JMP;  
      case 0x1: return JN;   
      case 0x2: return JZ;
      case 0x4: return LOAD; 
      case 0x5: return STORE;
      case 0x6: return LOADI;
      case 0x7: return STOREI;
      case 0x8: return AND;
      case 0x9: return OR;
      case 0xA: return ADD;  
      case 0xB: return SUB;
      case 0xF: return HALT;
      default:  return NOP;
    }
  }
}
