package Entities;

import java.util.List;
import java.util.Map;

public class NumberExpression extends Expression {
    private final int value;

    public NumberExpression(int value) {
        super(Type.INTEGER);
        this.value = value;
    }

    public Object evaluate(List<Function> functions, Map<Parameter, Object> parameters) {
        return value;
    }
}