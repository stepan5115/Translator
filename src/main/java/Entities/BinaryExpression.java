package Entities;

import java.util.List;
import java.util.Map;

public class BinaryExpression extends Expression {
    private final Expression left;
    private final Expression right;
    private final char operator;

    public BinaryExpression(Expression left, char operator, Expression right) {
        super(Type.INTEGER);
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    public Object evaluate(List<Function> functions, Map<Parameter, Object> ctx) {
        Object lval = left.evaluate(functions, ctx);
        Object rval = right.evaluate(functions, ctx);

        if (!(lval instanceof Integer) || !(rval instanceof Integer)) {
            throw new RuntimeException("Недопустимые типы для оператора " + operator +
                    ": " + lval.getClass().getSimpleName() + " и " + rval.getClass().getSimpleName());
        }

        int a = (Integer) lval;
        int b = (Integer) rval;

        return (operator == '+') ? a + b : a - b;
    }

    public Type getType() {
        return Type.INTEGER;
    }

    public Expression getLeft() { return left; }
    public Expression getRight() { return right; }
    public char getOperator() { return operator; }
}