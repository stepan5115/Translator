package Entities;

import java.util.List;
import java.util.Map;

//выражение, которое содержит два под-выражения и оператор
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

    //вычисляет результат выражения
    public Object evaluate(List<Function> functions, Map<Parameter, Object> ctx) {
        //вычисляет выражения слева и справа
        Object lval = left.evaluate(functions, ctx);
        Object rval = right.evaluate(functions, ctx);
        //проверяем тип вычисленных значений слева и справа (операторы применимы только к int)
        if (!(lval instanceof Integer) || !(rval instanceof Integer)) {
            throw new RuntimeException("Недопустимые типы для оператора " + operator +
                    ": " + lval.getClass().getSimpleName() + " и " + rval.getClass().getSimpleName());
        }
        //приводи тип к int и возвращаем сумму или разность (в зависимости от оператора)
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

    @Override
    public String toString() {
        return "..." + operator + "..." + "-" + type;
    }

    @Override
    public void correctionOfTypes(Map<Parameter, Object> globalContext, List<Function> functions, Function main) {
        if (left != null)
            left.correctionOfTypes(globalContext, functions, main);
        else
            throw new RuntimeException("В выражении с оператором левая часть пустая!");
        if (right != null)
            right.correctionOfTypes(globalContext, functions, main);
        else
            throw new RuntimeException("В выражении с оператором правая часть пустая!");
        if (right.getType() != left.getType())
            throw new RuntimeException("Выявлено несовпадение типов в сложном выражении: " +
                    left.getType() + operator + right.getType());
        if (right.getType() != Type.INTEGER)
            throw new RuntimeException("Выявлено несовпадение типов в сложном выражении: можно " +
                    "производить операции \"" + operator + "\" только над выражениями типа INTEGER");
    }
}