package Entities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpressionFunctionCall extends Expression {
    private final String functionName;
    private final List<Expression> arguments;

    public String getFunctionName() {
        return functionName;
    }
    public List<Expression> getArguments() {
        return arguments;
    }

    public ExpressionFunctionCall(String functionName, List<Expression> arguments, Type returnType) {
        super(returnType);
        this.functionName = functionName;
        this.arguments = arguments;
    }

    public Object evaluate(List<Function> functions, Map<Parameter, Object> globalContext) {
        Function func = null;
        for (Function function : functions)
            if (function.getName().equals(functionName)) {
                func = function;
                break;
            }
        if (func == null) {
            throw new RuntimeException("Функция не найдена: " + functionName);
        }
        List<Parameter> params = func.getParameters();
        if (params.size() != arguments.size()) {
            throw new RuntimeException("Неверное количество аргументов при вызове функции " + functionName);
        }

        Map<Parameter, Object> localContext = new HashMap<>();
        for (int i = 0; i < params.size(); i++) {
            Object value = arguments.get(i).evaluate(functions, globalContext);
            localContext.put(params.get(i), value);
        }

        for (Instruction instr : func.getInstructions()) {
            instr.execute(functions, localContext);
        }

        if (func.getReturnInstruction() != null) {
            return func.getReturnInstruction().evaluate(functions, localContext);
        }

        return null;
    }
}