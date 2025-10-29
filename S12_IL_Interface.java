/**
 * Interface specifying required methods for Instruction level simulator for S12
 * ISA
 * Please name your file that implements this interface S12_IL.java
 * 
 * @author sfrost
 * @date Fall 2025
 */
public interface S12_IL_Interface {

    /*
     * Note: Don't forget your constructors!
     */

    /**
     * initializeMem reads in the plain text file and instantiates the memory array
     * 
     * @param filename of memory to be read in
     * @return true if memory successfully parsed and instantiated
     */
    public boolean intializeMem(String filename);

    /**
     * Returns state of the registers in the machine
     * This is one place to add a javadoc in your implementation.
     * Specifying in what order the register values are returned
     * 
     * @return String array of register values
     */
    public String[] getProcessorState();

    /**
     * String form of the current state of the memory (e.g. each line is hex
     * address, space, binary word)
     * 
     * @return string representation of memory
     */
    public String getMemState();

    /**
     * execute one cycle of the machine
     * 
     * @return String representation of the instruction executed (binary)
     */
    public String update();

    /**
     * Write out the memFile associated with the current state of the simulation
     * 
     * @param filename - name of memFile to create
     * @return true if successful file creation, false otherwise
     */
    public boolean writeMem(String filename);

    /**
     * 
     * @param filename - name of trace file to create
     * @return true if successfully written, else false
     */
    public boolean writeTrace(String filename);

    // You don't have to do these, but I'd suggest it. It will make your life
    // easier.
    // /**
    // * Translate s12 assembly instruction into binary
    // * @param s12Instr human readable instruction, space, two hex digits
    // * @return String form of twelve bits (e.g. {w over {0,1} | |w| is 12})
    // */
    // public String s12ToBinary(String s12Instr);

    // /**
    // * Translate binary word into human readable instruction and two hex digits
    // * @param binInstr
    // * @return String of human readable instruction and two hex digits
    // * e.g. 000010000001 --> JMP A1
    // */
    // public String binaryToS12(String binInstr);

}
