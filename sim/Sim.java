package sim;

import core.*;
import io.*;
import pipe.S12Pipe;

import java.nio.file.*;
import java.util.List;

public final class Sim {
  public static void main(String[] args) throws Exception {
    if (args.length < 1) { 
      System.err.println("usage: sim <memFile> [-o base] [-c cycles] [--no-fwd]");
      System.exit(1); 
    }

    String memFile = args[0]; 
    Integer maxCycles = null; 
    boolean fwd = true;

    for (int i=1;i<args.length;i++){
      switch(args[i]){
        case "-c":  maxCycles = Integer.parseInt(args[++i]);
                    break;
        case "--no-fwd": fwd = false;
                         break;
        default: throw new IllegalArgumentException("Unknown arg "+args[i]);
      }
    }

    MemImage img = MemIO.read(Paths.get(memFile));
    Cpu cpu = new S12Pipe();
    cpu.reset(); 
    cpu.loadMemory(img.mem);
    cpu.setState(img.pc, img.acc); 
    cpu.setForwardingEnabled(fwd);

    cpu.setTraceSink(null);
    cpu.run(maxCycles);

    List<String> trace = cpu.drainRetireTrace();
    for (String line: trace) {
      System.out.println(line);
    }
    printStats(cpu, memFile);
  }

  private static final String[][] OPCODE_ORDER = new String[][]{
    {"0", "JMP"},
    {"1", "JN"},
    {"2", "JZ"},
    {"4", "LOAD"},
    {"5", "STORE"},
    {"6", "LOADI"},
    {"7", "STOREI"},
    {"8", "AND"},
    {"9", "OR"},
    {"A", "ADD"},
    {"B", "SUB"},
    {"F", "HALT"},
  };
  
  private static int hexNibbleToIndex(String h) {
    return Integer.parseInt(h, 16) & 0xF;
  }
  
  private static void printInstructionMix(long[] mix){
    System.out.println("Instruction Mix:");
    for (String[] pair : OPCODE_ORDER){
      String hex = pair[0];
      String name = pair[1];
      long count = mix[hexNibbleToIndex(hex)];
      if (count > 0){
        System.out.printf("  %-7s %d%n", name + ":", count);
      }
    }
  }

  private static void printStats(Cpu c, String base){
    System.out.println("=== S12 Pipeline Run ==");
    System.out.printf("Base:       %s%n", base);
    System.out.printf("Cycles:   %d%n", c.getCycles());
    System.out.printf("Retired:%d%n", c.getRetired());
    System.out.printf("PC:     0x%02X%nACC:  0x%03X%n", c.getPC(), c.getACC());
    System.out.printf("Stalls:   %d%n", c.getStalls());
    System.out.printf("Fwd EX->EX: %d%nFwd MEM->Ex: %d%n", c.getFwdEXtoEX(), c.getFwdMEMtoEX());
    printInstructionMix(c.getInstructionMix());
  }  
  
}
