package Entities;

import java.util.List;
import java.util.Map;

//реализация инструкции как вызова функции
public class FunctionCallInstruction extends Instruction {
    private final ExpressionFunctionCall call;

    public FunctionCallInstruction(ExpressionFunctionCall call) {
        this.call = call;
    }
    //исполнение инструкции
    public void execute(List<Function> functions, Map<Parameter, Object> context) {
        //запускаем вычисление функции (выполняет все инструкции внутри и возвращает значение)
        //возвращаемое значение просто игнорируем
        call.evaluate(functions, context);
    }

    public ExpressionFunctionCall getCall() {
        return call;
    }
}