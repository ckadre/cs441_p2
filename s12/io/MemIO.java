package io;

import java.nio.file.*;
import java.io.*;
import java.util.*;

public final class MemIO {

  public static MemImage read(Path p) throws IOException {
    List<String> lines = Files.readAllLines(p);
    if (lines.isEmpty()) throw new IOException("Empty mem file: " + p);

    MemImage img = new MemImage();

    // Find first non-empty, non-comment line as header: "PC8 ACC12"
    int i = 0;
    String header = null;
    while (i < lines.size()) {
      String s = stripComment(lines.get(i++));
      if (!s.isEmpty()) { header = s; break; }
    }
    if (header == null) throw new IOException("Missing header (PC ACC).");

    String[] head = splitWS(header);
    if (head.length < 2) throw new IOException("Invalid header (need PC and ACC): " + header);
    img.pc  = parse8(head[0]);
    img.acc = parse12(head[1]);

    // Parse up to 256 address/value lines (skip comments/blanks)
    int loaded = 0;
    while (i < lines.size() && loaded < 256) {
      String s = stripComment(lines.get(i++));
      if (s.isEmpty()) continue;

      String[] tok = splitWS(s);
      if (tok.length < 2)
        throw new IOException("Bad mem line (need <addr> <value>): " + s);

      int addr = parse8(tok[0]);
      int val  = parse12(tok[1]);
      img.mem[addr & 0xFF] = val & 0xFFF;
      loaded++;
    }
    return img;
  }

  public static void write(Path out, int pc, int acc, int[] mem) throws IOException {
    try (BufferedWriter w = Files.newBufferedWriter(out)) {
      w.write(bin8(pc) + " " + bin12(acc));
      w.newLine();
      for (int a = 0; a < 256; a++) {
        w.write(String.format("%02X %s", a, bin12(mem[a])));
        w.newLine();
      }
    }
  }

  // --- helpers ---
  private static String stripComment(String s) {
    int i = s.indexOf("//");
    if (i >= 0) s = s.substring(0, i);
    return s.trim();
  }
  private static String[] splitWS(String s){ return s.trim().split("\\s+"); }

  private static int parse8(String s){
    s = s.trim();
    if (s.matches("[01]{8}")) return Integer.parseInt(s, 2) & 0xFF;            // binary 8
    if (s.startsWith("0x")||s.startsWith("0X")) return Integer.parseInt(s.substring(2), 16) & 0xFF;
    if (s.matches("[0-9A-Fa-f]{1,2}")) return Integer.parseInt(s, 16) & 0xFF;  // hex 1–2
    return Integer.parseInt(s) & 0xFF;                                         // decimal fallback
  }

  private static int parse12(String s){
    s = s.trim();
    if (s.matches("[01]{12}")) return Integer.parseInt(s, 2) & 0xFFF;          // binary 12
    if (s.startsWith("0x")||s.startsWith("0X")) return Integer.parseInt(s.substring(2), 16) & 0xFFF;
    if (s.matches("[0-9A-Fa-f]{1,3}")) return Integer.parseInt(s, 16) & 0xFFF; // hex 1–3
    return Integer.parseInt(s) & 0xFFF;                                        // decimal fallback
  }

  private static String bin8(int x){  return String.format("%8s",  Integer.toBinaryString(x & 0xFF)).replace(' ', '0'); }
  private static String bin12(int x){ return String.format("%12s", Integer.toBinaryString(x & 0xFFF)).replace(' ', '0'); }
}
