package Entities;

import java.util.List;
import java.util.Map;

//реализация выражения как обращение к переменной
public class VariableExpression extends Expression {
    private final String name;
    public String getName() {
        return name;
    }

    public VariableExpression(String name, Type type) {
        super(type);
        this.name = name;
    }
    //вычисление значения выражения
    public Object evaluate(List<Function> functionList, Map<Parameter, Object> parameters) {
        //проверяем существует и идентификатор в контексте
        for (Parameter parameter : parameters.keySet())
            if (parameter.getName().equals(name)) {
                //при совпадении проверяем совпадение типов
                //так же тип не может быть void
                if (parameter.getType() == Type.VOID)
                    throw new RuntimeException("Ошибка, параметр типа void: " + parameter.getName());
                if (!(parameters.get(parameter) instanceof Integer))
                    throw new RuntimeException("В параметр типа " + type + " передано значение другого типа: " +
                            parameters.get(parameter).toString());
                return parameters.get(parameter);
            }
        throw new RuntimeException("Неизвестная переменная: " + name);
    }
}