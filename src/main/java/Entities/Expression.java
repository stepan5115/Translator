package Entities;

import java.util.List;
import java.util.Map;

//абстрактный класс для представления выражения
public abstract class Expression {
    protected Type type;
    public Expression(Type type) {
        this.type = type;
    }
    public Type getType() {
        return type;
    }
    public void setType(Type type) {
        this.type = type;
    }
    public abstract Object evaluate(List<Function> functions, Map<Parameter, Object> parameters);

    //из-за особенностей реализации в процессе парсинга
    //выражение еще ничего не знает о глобальных функциях
    //отсюда может проиойти неправльное определение типа выражения
    //поэтому введена дополнительная проверка типов перед вычислением
    public void correctionOfTypes(Map<Parameter, Object> globalContext, List<Function> functions, Function main) {
        //реализация по умолчанию (например для простого числа)
        //считаем тип правильным
    }
}
