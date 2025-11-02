package pipe;
import isa.Ctrl;
import isa.Op;

public final class Latches {

  public static final class IF_ID { 
    public int pc, instr; 
    public boolean valid;
    public void clear() { pc = instr = 0; valid = false; }
    public static IF_ID bubble() { 
      var l = new IF_ID();
      l.valid = false;
      return l;
     }
  }

  public static final class ID_EX { 
    public int pc,instr,X,accVal; 
    public Op op; 
    public Ctrl ctrl;
    public boolean valid; 
    public void clear() {
      pc = instr = X = accVal = 0;
      op = Op.NOP;
      ctrl = Ctrl.nop();
      valid = false;
    }
    public static ID_EX bubble() {
      var l = new ID_EX();
      l.clear();
      return l;
    }
  }

  public static final class EX_MEM { 
    public int pc,memAddr,aluRes,storeData; 
    public Op op; 
    public Ctrl ctrl; 
    public boolean valid;
    
    public boolean branchTaken;
    public int branchTarget;

    public void clear() {
      pc = memAddr = aluRes = storeData = 0;
      op = Op.NOP;
      ctrl = Ctrl.nop();
      valid = false;
    }
    public static EX_MEM bubble() {
      var l = new EX_MEM();
      l.clear();
      return l;
    }
  }

  public static final class MEM_WB{ 
    public int pc,wbData; 
    public Op op; 
    public Ctrl ctrl; 
    public boolean valid;

    public boolean didMemWrite;
    public int effAddr;
    public int storeVal;
    public boolean branchTaken;
    public int branchTarget;

    public void clear() {
      pc = wbData = 0;
      op = Op.NOP;
      ctrl = Ctrl.nop();
      valid = false;
    }
    public static MEM_WB bubble() {
      var l = new MEM_WB();
      l.clear();
      return l;
    }
  }
}
