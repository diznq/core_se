package exchange.vm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Compiler {

    public static Script compile(String code) {
        Scope activeScope = new Scope();
        final List<SemiInstruction> semiInstructions = new ArrayList<>();
        final String[] lines = code.split("\n");

        for (String line : lines) {
            line = line.trim().split("#")[0];
            if (line.length() == 0) continue;
            String[] tokens = line.split("\\s+");
            int i = 0;
            String imm = "0";
            Opcode opcode = null;
            Condition condition = Condition.NONE;
            for (String token : tokens) {
                if (token.equals("{")) {
                    activeScope = activeScope.createChild();
                } else if (token.equals("}")) {
                    activeScope = activeScope.parent;
                } else if (token.startsWith(":")) {
                    if (i == 0) {
                        activeScope.put(token, semiInstructions.size());
                    } else {
                        imm = token;
                    }
                } else if (token.startsWith("$")) {
                    imm = token;
                } else {
                    if (opcode == null) {
                        if (token.length() >= 2) {
                            try {
                                condition = Condition.valueOf(token.substring(token.length() - 2));
                                token = token.substring(0, token.length() - 2);
                            } catch (IllegalArgumentException ex) {
                                condition = Condition.NONE;
                            }
                        }
                        opcode = Opcode.valueOf(token.toUpperCase());
                    } else {
                        imm = token;
                    }
                }
                i++;
            }
            if (opcode != null) {
                SemiInstruction semi = new SemiInstruction();
                semi.condition = condition;
                semi.opcode = opcode;
                semi.imm = imm;
                semi.scope = activeScope;
                semiInstructions.add(semi);
            }
        }
        List<Instruction> instructions = semiInstructions.stream().map(inst -> {
            Instruction real = new Instruction();
            real.opcode = inst.opcode;
            real.condition = inst.condition;
            if (inst.imm.startsWith(":")) {
                Scope scope = inst.scope.parent != null ? inst.scope.parent : inst.scope;
                real.value = scope.getIndex(inst.imm, false);
            } else if (inst.imm.startsWith("$")) {
                real.value = inst.scope.getIndex(inst.imm, true);
            } else {
                real.value = Integer.parseInt(inst.imm);
            }
            return real;
        }).toList();
        Script script = new Script();
        script.instructions = instructions;
        return script;
    }

    static class SemiInstruction {
        Opcode opcode;
        Condition condition;
        String imm;
        Scope scope;
    }

    static class Scope {
        static int scopeCounter = 0;
        int scopeId = scopeCounter++;
        int varCounter = 0;
        Map<String, Integer> labels = new TreeMap<>();
        Scope parent;

        Integer getIndex(String label, boolean create) {
            if (labels.containsKey(label)) {
                return labels.get(label);
            } else {
                if (parent != null) {
                    Integer parentIdx = parent.getIndex(label, false);
                    if (parentIdx != null) {
                        return parentIdx;
                    }
                }
            }
            if (create) {
                int offset = getOffset();
                labels.put(label, offset);
                varCounter++;
                return offset;
            } else {
                return null;
            }
        }

        Scope createChild() {
            Scope scope = new Scope();
            scope.varCounter = 0;
            scope.parent = this;
            return scope;
        }

        int getOffset() {
            if (parent == null) return varCounter;
            return varCounter + parent.getOffset();
        }

        void put(String key, Integer value) {
            labels.put(key, value);
        }
    }
}
