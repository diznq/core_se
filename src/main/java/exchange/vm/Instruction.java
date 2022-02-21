package exchange.vm;

public class Instruction {
    Opcode opcode = Opcode.EXIT;
    Condition condition = Condition.NONE;
    int value = 0;

    public Opcode getOpcode() {
        return opcode;
    }

    public Condition getCondition() {
        return condition;
    }

    public int getValue() {
        return value;
    }
}
