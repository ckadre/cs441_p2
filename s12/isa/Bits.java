package isa;

public final class Bits {
  public static int m8(int x){return x & 0xFF;}
  public static int m12(int x){return x & 0xFFF;}
  public static boolean isNeg12(int x){ return (x & 0x800)!=0; }
}

