package Entities;

import java.util.List;
import java.util.Map;

public class FunctionCallInstruction extends Instruction {
    private final ExpressionFunctionCall call;

    public FunctionCallInstruction(ExpressionFunctionCall call) {
        this.call = call;
    }

    public void execute(List<Function> functions, Map<Parameter, Object> context) {
        call.evaluate(functions, context);
    }

    public ExpressionFunctionCall getCall() {
        return call;
    }
}