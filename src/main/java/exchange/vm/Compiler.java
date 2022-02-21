package exchange.vm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Compiler {

    static class SemiInstruction {
        Opcode opcode;
        Condition condition;
        String imm;
    }

    public static Script compile(String code){
        int varCounter = 0;
        final List<SemiInstruction> semiInstructions = new ArrayList<>();
        final Map<String, Integer> labels = new TreeMap<>();
        final String[] lines = code.split("\n");
        for(String line : lines){
            line = line.trim().split("#")[0];
            if(line.length() == 0) continue;
            String[] tokens = line.split("\\s+");
            int i = 0;
            String imm = "0";
            Opcode opcode = null;
            Condition condition = Condition.NONE;
            for(String token : tokens){
                if(token.startsWith(":")){
                    if(i == 0){
                        labels.put(token, semiInstructions.size());
                    } else {
                        imm = token;
                    }
                } else if(token.startsWith("$")){
                    if(!labels.containsKey(token)){
                        labels.put(token, varCounter++);
                    }
                    imm = token;
                } else {
                    if(opcode == null){
                        if(token.length() >= 2){
                            try {
                                condition = Condition.valueOf(token.substring(token.length() - 2));
                                token = token.substring(0, token.length() - 2);
                            } catch(IllegalArgumentException ex){
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
            if(opcode != null) {
                SemiInstruction semi = new SemiInstruction();
                semi.condition = condition;
                semi.opcode = opcode;
                semi.imm = imm;
                semiInstructions.add(semi);
            }
        }
        List<Instruction> instructions = semiInstructions.stream().map(inst -> {
            Instruction real = new Instruction();
            real.opcode = inst.opcode;
            real.condition = inst.condition;
            if (labels.containsKey(inst.imm)) {
                real.value = labels.get(inst.imm);
            } else {
                real.value = Integer.parseInt(inst.imm);
            }
            return real;
        }).toList();
        Script script = new Script();
        script.instructions = instructions;
        return script;
    }
}
