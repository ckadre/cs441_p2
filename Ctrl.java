public final class Ctrl {
    boolean RegWrite;
    boolean MemRead;
    boolean MemWrite;
    boolean UseImm;
    int ALUOp;
    boolean Branch;
    boolean Jump;
}

final class IF_ID {
    int pc;
    int instr;
    boolean valid;
}

final class ID_EX {
    int pc;
    int opcode;
    int X;
    int accVal;
    Ctrl ctrl;
    boolean valid;
}

final class EX_MEM {
    int aluRes;
    int memAddr;
    int storeData;
    Ctrl ctrl;
    boolean valid;
}

final class MEM_WB {
    int wbData;
    Ctrl ctrl;
    boolean valid;
}