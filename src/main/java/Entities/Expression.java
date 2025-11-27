package Entities;

import java.util.List;
import java.util.Map;

//абстрактный класс для представления выражения
public abstract class Expression {
    protected final Type type;
    public Expression(Type type) {
        this.type = type;
    }
    public Type getType() {
        return type;
    }
    public abstract Object evaluate(List<Function> functions, Map<Parameter, Object> parameters);
}
