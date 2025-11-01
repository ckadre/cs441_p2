public class S12PipeCPU {
    int PC = 0; //8-bits
    int ACC = 0;//12-bits
    int[] mem = new int[256]; //12-bits

    boolean halted = false;

    //Stats for benchmarks
    long cycles = 0;
    long instrCount = 0;
    long stallCount = 0;
    long fwdEXCount = 0;
    long fwdMEMCount = 0;

    //Pipeline latches
    IF_ID   if_id = new IF_ID();
    ID_EX   id_ex = new ID_EX();
    EX_MEM  ex_mem =new EX_MEM();
    MEM_WB  mem_wb =new MEM_WB();

    static int mask8(int x) { return x & 0xFF; }
    static int mask12(int x) { return x & 0xFFF; }
}
