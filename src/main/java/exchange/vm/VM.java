package exchange.vm;

import java.util.List;

public class VM {
    int zf = 0;
    int stackPointer = 0;
    int callStackPointer = 0;
    int instructionPointer = 0;
    int[] memory = new int[256];
    int[] stack = new int[256];
    int[] callStack = new int[256];
    int executedInstructions = 0;
    long startTime = 0L;
    long executionTime = 0L;
    boolean halted = false;

    public static VM exec(Script script){
        VM vm = new VM();
        vm.run(script);
        return vm;
    }

    public void run(Script script){
        startTime = System.currentTimeMillis();
        List<Instruction> instructions = script.instructions;
        int maxExecuted = script.maxInstructions;
        while(!halted && instructionPointer < instructions.size() && (maxExecuted == 0 || executedInstructions < maxExecuted)){
            exec(instructions.get(instructionPointer));
            instructionPointer += 1;
            executedInstructions++;
        }
        executionTime = System.currentTimeMillis() - startTime;
    }

    private void exec(Instruction inst){
        int a = 0;
        int b = 0;
        switch(inst.condition){
            case EQ:
                if(zf != 0) return;
                break;
            case LT:
                if(zf >= 0) return;
                break;
            case GT:
                if(zf <= 0) return;
                break;
            case LE:
                if(zf > 0) return;
                break;
            case GE:
                if(zf < 0) return;
                break;
            case NE:
                if(zf == 0) return;
                break;
            case NONE:
                break;
        }
        switch(inst.opcode){
            case ADD:
                push(pop() + pop());
                break;
            case MUL:
                push(pop() * pop());
                break;
            case SUB:
                b = pop(); a = pop();
                push(a - b);
                break;
            case DIV:
                b = pop(); a = pop();
                push(a / b);
                break;
            case MOD:
                b = pop(); a = pop();
                push(a % b);
                break;
            case LOADK:
                push(inst.value);
                break;
            case LOAD:
                push(memory[inst.value]);
                break;
            case STORE:
                memory[inst.value] = pop();
                break;
            case DUP:
                a = pop();
                push(a);
                push(a);
                break;
            case DUP2:
                b = pop();
                a = pop();
                push(a); push(b);
                push(a); push(b);
                break;
            case CMP:
                b = pop();
                a = pop();
                zf = Integer.compare(a, b);
                break;
            case BR:
                instructionPointer = inst.value - 1;
                break;
            case CALL:
                callStack[callStackPointer++] = instructionPointer;
                instructionPointer = inst.value - 1;
                break;
            case RET:
                instructionPointer = callStack[--callStackPointer];
                break;
            case EXIT:
                halted = true;
                break;
        }
    }

    void push(int value) {
        stack[stackPointer++] = value;
        zf = Integer.compare(value, 0);
    }

    int pop() {
        int result = stack[--stackPointer];
        if(stackPointer > 0)
            zf = Integer.compare(stack[stackPointer - 1], 0);
        return result;
    }

    public Integer top(){
        if(stackPointer == 0) return null;
        return stack[stackPointer - 1];
    }

    public ScriptResult result() {
        ScriptResult res = new ScriptResult();
        res.result = top();
        res.instructions = executedInstructions;
        res.time = executionTime;
        return res;
    }

    public boolean isHalted() {
        return halted;
    }
}
