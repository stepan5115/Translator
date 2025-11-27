package Entities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//реализация выражения как вызов функции
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

    //вычисление значения (требуется контекст для получения интересующей функции)
    public Object evaluate(List<Function> functions, Map<Parameter, Object> globalContext) {
        //ищем функцию в контексте
        Function func = null;
        for (Function function : functions)
            if (function.getName().equals(functionName)) {
                func = function;
                break;
            }
        if (func == null) {
            throw new RuntimeException("Функция не найдена: " + functionName);
        }
        //проверяем соответствие числа параметров числу переданных аргументов
        List<Parameter> params = func.getParameters();
        if (params.size() != arguments.size()) {
            throw new RuntimeException("Неверное количество аргументов при вызове функции " + functionName);
        }
        //создаем контекст (локальный + глобальный)
        //локальные - параметры функции
        Map<Parameter, Object> localContext = new HashMap<>();
        for (int i = 0; i < params.size(); i++) {
            //вычисляем значение каждого выражения, чтобы подставить вычисленные значения
            //формируем множество пар параметр-вычисленное значение
            Object value = arguments.get(i).evaluate(functions, globalContext);
            localContext.put(params.get(i), value);
        }
        //пытаемся выполнить каждую инструкцию в теле функции
        for (Instruction instr : func.getInstructions()) {
            instr.execute(functions, localContext);
        }
        //если есть возвращаемое выражение, то возвращаем его вычисленный результат
        if (func.getReturnInstruction() != null) {
            return func.getReturnInstruction().evaluate(functions, localContext);
        }
        //иначе ничего не возвращаем
        return null;
    }
}