package Entities;

import java.util.List;
import java.util.Map;

//реализация выражения как обычного числа
public class NumberExpression extends Expression {
    private final int value;

    public NumberExpression(int value) {
        super(Type.INTEGER);
        this.value = value;
    }
    //вычисление значения выражения
    public Object evaluate(List<Function> functions, Map<Parameter, Object> parameters) {
        return value;
    }

    @Override
    public String toString() {
        return value + "-" + type;
    }
}