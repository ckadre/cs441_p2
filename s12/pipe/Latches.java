package pipe;
import isa.Ctrl;
import isa.Op;

/**
 * Pipeline register bundle for the 5-stage S12 pipeline.
 *
 * Each nested class models the latch between two adjacent stages and carries
 * exactly the data that later stages need plus. All
 * latches support clear() to inject a bubble and a bubble()
 * helper to construct one in-place.
 *
 * Design notes:
 * - Fields are public for low-friction, stage-local read/write in a teaching
 *   simulator; encapsulation would add setters/getters without benefit here.
 * - Default/bubble state uses {@code Op.NOP} and {@code Ctrl.nop()} so later
 *   logic can uniformly check control bits without null guards.
 */

public final class Latches {

  /**
   * IF/ID latch: output of Fetch, input to Decode.
   * Carries the fetched instruction word and its PC, plus a valid bit.
   */
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

  /**
   * ID/EX latch: output of Decode, input to Execute.
   * Carries decoded opcode, control, decoded fields, and the ACC snapshot.
   */

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

  /**
   * EX/MEM latch: output of Execute, input to Memory.
   * Carries computed ALU/pass results, store data, and branch resolution.
   */
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

  /**
   * MEM/WB latch: output of Memory, input to Writeback.
   * Carries writeback data and side-band info for tracing.
   */

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
