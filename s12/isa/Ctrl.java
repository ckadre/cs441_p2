package isa;

public final class Ctrl {
  public boolean RegWrite, MemRead, MemWrite, BranchJmp, UseIndir;
  public Alu alu = Alu.NONE;
  public Op op = Op.NOP;
  
  public static Ctrl nop(){ return new Ctrl(); }

  public static Ctrl copyOf(Ctrl c) {
    if(c == null) return nop();
    Ctrl d = new Ctrl();
    d.RegWrite = c.RegWrite;
    d.MemRead = c.MemRead;
    d.MemWrite = c.MemWrite;
    d.BranchJmp = c.BranchJmp;
    d.UseIndir = c.UseIndir;
    d.alu = c.alu;
    d.op = c.op;
    return d;
  }

  @Override public String toString() {
    return String.format("Ctrl{ops=%s, alu=%s, RW=%s, MR=%s, MW=%s, BJ=%s, IND=%s}",
      op, alu, RegWrite, MemRead, MemWrite, BranchJmp, UseIndir);
  }
}
