package sim;

import core.*;
import io.*;
import pipe.S12Pipe;
import isa.Op;

import java.nio.file.*;

/**
 * Runs the S12 CPU simulator.
 *
 * Loads a 12-bit memory image, initializes the CPU, executes instructions until HALT
 * or until a specified cycle limit is reached, and reports final stats.
 *
 * Usage:
 *   sim <memFile> [-o <base>] [-c <cycles>] [--no-fwd]
 *
 * Arguments:
 *   <memFile>   Path to a .mem file in format (first line: PC/ACC).
 *   -o <base>   Write trace output and final memory image using <base> as filename prefix.
 *   -c <cycles> Stop after the given number of cycles, even if HALT not reached.
 *   --no-fwd    Disable data forwarding.
 *
 * Behavior:
 *   • If -o is used, trace output is written to files and final memory to <base>.mem.
 *   • Otherwise, retire trace lines are printed to stdout.
 *   • Instruction mix and pipeline stats are always printed at the end.
 *
 * Exit codes:
 *   0  = normal completion
 *   >0 = invalid arguments or I/O error
 */

public final class Sim {

  /**
   * Launches the simulator.
   *
   * @param args command-line arguments. See usage.
   * @throws Exception if file IO fails or if the CPU implementation throws during run.
   */
  public static void main(String[] args) throws Exception {
    if (args.length < 1) { 
      System.err.println("usage: sim <memFile> [-o base] [-c cycles] [--no-fwd]");
      System.exit(1); 
    }

    // Parse arguments
    String memFile = args[0]; 
    Integer maxCycles = null; 
    boolean fwd = true;
    String outBase = null;

    for (int i=1;i<args.length;i++){
      switch(args[i]){
        case "-o": outBase = args[++i]; 
                  break;
        case "-c":  maxCycles = Integer.parseInt(args[++i]);
                    break;
        case "--no-fwd": fwd = false;
                         break;
        default: throw new IllegalArgumentException("Unknown arg "+args[i]);
      }
    }

    // Load memory image and configure CPU
    MemImage img = MemIO.read(Paths.get(memFile));
    Cpu cpu = new S12Pipe();
    cpu.reset();
    cpu.loadMemory(img.mem);
    cpu.setState(img.pc, img.acc);
    cpu.setForwardingEnabled(fwd);
    
    // Run and record output
    if (outBase != null) {
      try (var sink = new FileTraceSink(outBase)) {
        cpu.setTraceSink(sink);
        cpu.run(maxCycles);
      }

      MemIO.write(Paths.get(outBase + ".mem"), cpu.getPC(), cpu.getACC(), cpu.getMem());
    } else {
      cpu.setTraceSink(null);
      cpu.run(maxCycles);
      for (String line : cpu.drainRetireTrace()) {
        System.out.println(line);
      }
    }
    
    printStats(cpu, outBase != null ? outBase : memFile);
  }

  /**
  * Prints a summary of how many times each instruction type retired.
  *
  * @param mix array of instruction counts indexed by opcode
  */
  private static void printInstructionMix(long[] mix) {
    System.out.println("Instruction Mix:");
    for (Op op : Op.values()) {
      if (op == Op.NOP) continue;

      long count = mix[op.code & 0xF];
      if (count > 0)
        System.out.printf("  %-7s %d%n", op.name() + ":", count);
    }
  }

  /**
   * Prints a formatted summary of CPU performance statistics after a run.
   *
   * @param c the Cpu instance that was executed
   * @param base the base name of the input program
   */
  private static void printStats(Cpu c, String base){
    System.out.printf("S12 Pipeline %s:", base);
    System.out.printf("Cycles:   %d%n", c.getCycles());
    System.out.printf("Stalls:   %d%n", c.getStalls());
    System.out.printf("Fwd MEM->Ex: %d%n", c.getFwdMEMtoEX());
    printInstructionMix(c.getInstructionMix());
  }  
  
}
