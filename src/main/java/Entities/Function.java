package Entities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//класс, представляющий функцию
public class Function {
    private Type type;
    private String name;
    private List<Parameter> parameters;
    private List<Instruction> instructions;
    private Expression returnInstruction;

    public Function(Type type, String name, List<Parameter> parameters,
                    List<Instruction> instructions, Expression returnInstruction) {
        this.type = type;
        this.name = name;
        this.parameters = parameters != null ? parameters : List.of();
        this.instructions = instructions != null ? instructions : List.of();
        //сверяем совпадение типа значения, возвращаемого return и типа функции
        if (returnInstruction == null) {
            if (type == Type.VOID)
                this.returnInstruction = null;
            else
                throw new RuntimeException("Выявлено несовпадение типов в функции " +
                        name + ". Ожидалось: " + type.toString() +
                        ", получилось: void");
        }
        else if ((returnInstruction.getType() == Type.VOID) && (type == Type.VOID))
            this.returnInstruction = null;
        else if ((returnInstruction.getType() == type))
            this.returnInstruction = returnInstruction;
        else
            throw new RuntimeException("Выявлено несовпадение типов в функции " +
                    name + ". Ожидалось: " + type.toString() +
                    ", получилось: " + returnInstruction.getType());
    }

    public Type getType() {
        return type;
    }
    public String getName() {
        return name;
    }
    public List<Parameter> getParameters() {
        return parameters;
    }
    public List<Instruction> getInstructions() {
        return instructions;
    }

    public Expression getReturnInstruction() {
        return returnInstruction;
    }

    //проверка контекста функции
    public void checkRightContext(List<Function> functions) {
        //создаем контекст переменных, заполняя его параметрами функции
        Map<String, Type> localContext = new HashMap<>();
        for (Parameter parameter : parameters)
            localContext.put(parameter.getName(), parameter.getType());
        //проверяем инструкции на соответствии контексту
        for (Instruction instr : instructions) {
            if (instr instanceof FunctionCallInstruction exprInstr) {
                resolveExpressionType(exprInstr.getCall(), functions, localContext);
            }
            else
                throw new RuntimeException("Неизвестная инструкция: " + instr);
        }
        //проверяем return на соответствие контексту
        //и снова на всякий случай проверяем соответствие типа return и функции
        if (returnInstruction != null) {
            Type retType = resolveExpressionType(returnInstruction, functions, localContext);
            if (retType != this.type)
                throw new RuntimeException("Ошибка: возвращаемое значение функции '" + name
                        + "' имеет тип " + retType + ", ожидалось " + this.type);
        } else if (this.type != Type.VOID) {
            throw new RuntimeException("Ошибка: функция '" + name + "' должна возвращать значение типа " + this.type);
        }
    }
    //сверяет соответствие контексту выражения и возвращает его тип
    public Type resolveExpressionType(Expression expr, List<Function> functions, Map<String, Type> localContext) {
        if (expr == null) return Type.VOID;

        //просто число
        if (expr instanceof NumberExpression) {
            return Type.INTEGER;
        }
        //обращение к переменной
        if (expr instanceof VariableExpression var) {
            String name = var.getName();
            if (!localContext.containsKey(name)) {
                boolean isFunction = functions.stream().anyMatch(f -> f.getName().equals(name));
                if (isFunction)
                    throw new RuntimeException("Ошибка: '" + name + "' — это функция, а не переменная");
                throw new RuntimeException("Ошибка: переменная '" + name + "' не объявлена в текущем контексте");
            }
            return localContext.get(name);
        }
        //вызов функции
        if (expr instanceof ExpressionFunctionCall call) {
            //поиск функции по имени в контексте
            String funcName = call.getFunctionName();
            Function f = functions.stream()
                    .filter(fn -> fn.getName().equals(funcName))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Ошибка: вызов неизвестной функции '" + funcName + "'"));
            //сверка количества аргументов и параметров
            List<Expression> args = call.getArguments();
            List<Parameter> params = f.getParameters();
            if (args.size() != params.size())
                throw new RuntimeException("Ошибка при вызове функции '" + funcName + "': ожидалось "
                        + params.size() + " аргументов, передано " + args.size());
            //сверка типов аргументов и типов параметров
            for (int i = 0; i < args.size(); i++) {
                Type argType = resolveExpressionType(args.get(i), functions, localContext);
                Type paramType = params.get(i).getType();
                if (argType != paramType)
                    throw new RuntimeException("Ошибка при вызове функции '" + funcName + "', аргумент "
                            + (i + 1) + ": ожидался " + paramType + ", получен " + argType);
            }
            return f.getType();
        }
        //композиция выражений
        if (expr instanceof BinaryExpression bin) {
            //проверка совпадения типов двух частей (и их равенство типу int)
            Type left = resolveExpressionType(bin.getLeft(), functions, localContext);
            Type right = resolveExpressionType(bin.getRight(), functions, localContext);
            if (bin.getLeft().type != bin.getRight().type)
                throw new RuntimeException("Несовместимые типы выражений: " +
                        left + " " + bin.getOperator() + " " + right);
            if (left != Type.INTEGER || right != Type.INTEGER)
                throw new RuntimeException("Ошибка: бинарная операция '" + bin.getOperator()
                        + "' возможна только над типами INTEGER");
            return Type.INTEGER;
        }

        throw new RuntimeException("Неизвестный тип выражения: " + expr.getClass().getSimpleName());
    }

}
