package core;

import java.util.List;

public interface Cpu {
  void reset();
  void loadMemory(int[] mem256);
  void setState(int pc8, int acc12);

  void setForwardingEnabled(boolean enabled);
  boolean isForwardingEnabled();

  void tick();                 // run exactly one cycle
  void run(Integer maxCycles); // or until HALT

  boolean isHalted();

  int  getPC(); 
  int getACC(); 
  int[] getMem();
  long getCycles(); 
  long getRetired(); 
  long getStalls();
  long getFwdEXtoEX(); 
  long getFwdMEMtoEX();
  long[] getInstructionMix();

  List<String> drainRetireTrace();
  void setTraceSink(TraceSink sink);
}

