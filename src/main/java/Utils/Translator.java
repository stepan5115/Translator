package Utils;

import Entities.*;
import Utils.Scanner.TOKEN;
import Utils.Scanner.TOKEN_TYPE;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Translator {
    private final Scanner scanner;
    private TOKEN currentToken;

    //----------------- поля-сборщики --------------------//
    private List<Instruction> currentInstructions;
    private Expression currentReturnExpression;
    private List<Parameter> currentParameters;

    //----------------- Глобальные списки --------------------//
    private final List<Function> definedFunctions = new LinkedList<>();
    private final List<Instruction> globalInstructions = new LinkedList<>();

    //----------------- Существующие функции --------------------/
    private final List<Function> existingFunctions = List.of(
            new Function(
                    Type.VOID,
                    "print",
                    List.of(new Parameter("int", "value")),
                    List.of(
                            new Instruction() {
                                public void execute(List<Function> functions, Map<Parameter, Object> context) {
                                    Object val = context.values().iterator().next();
                                    System.out.println(val);
                                }
                            }
                    ),
                    null
            )
    );

    public Translator(String sentence) {
        InputStream inputStream = new ByteArrayInputStream(sentence.getBytes(StandardCharsets.UTF_8));
        scanner = new Scanner(inputStream);
    }

    //----------------- служебные методы --------------------//
    //обновляет текущий токен при помощи сканера
    private void nextToken() {
        try {
            currentToken = scanner.nextToken();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка чтения токена: " + e.getMessage());
        }
    }
    //метод который упрощает проверку текущего метода на ожидаемый тип и значение
    //и выброс соответствующего исключения (для скобочек в основном)
    private void expect(TOKEN_TYPE type, Object value) {
        if (currentToken == null)
            throw new RuntimeException("Ожидался токен " + value + ", но поток закончился");
        if (currentToken.getType() != type || !currentToken.getValue().equals(value))
            throw new RuntimeException("Ожидался токен " + value + ", но найден " + currentToken.getValue());
        nextToken();
    }

    //----------------- грамматика --------------------//
    private void program() {
        nextToken();
        while (currentToken != null) {
            elementProgram();
        }
    }
    private void elementProgram() {
        //если обнаружили тип, значит элемент программы - объявление функции
        if (currentToken.getType() == TOKEN_TYPE.KEY_WORD &&
                (currentToken.asString().equals("int") || currentToken.asString().equals("void"))) {
            funcDeclaration();
        } else if (currentToken.getType() == TOKEN_TYPE.ID) {
        //если встретили идентификатор, то элемент программы - вызов функции
            ExpressionFunctionCall call = funcCallExpression();
            globalInstructions.add(ctxExec(call));
            expect(TOKEN_TYPE.SEPARATORS, ';');
        } else {
        //иначе - не соответствует синтаксису
            throw new RuntimeException("Ошибка в элементе программы: " + currentToken.getValue());
        }
    }
    private void funcDeclaration() {
        String typeName = currentToken.asString();
        nextToken();

        if (currentToken.getType() != TOKEN_TYPE.ID)
            throw new RuntimeException("Ожидалось имя функции, найдено: " + currentToken.getValue());
        String funcName = currentToken.asString();
        //проверяем, не объявлена ли уже данная функция пользователем
        //или не определена ли она уже в списке априори-существующих
        for (Function function : definedFunctions)
            if (function.getName().equals(funcName))
                throw new RuntimeException("Функция " + funcName + " уже объявлена");
        for (Function function : existingFunctions)
            if (function.getName().equals(funcName))
                throw new RuntimeException("Функция " + funcName + " уже существует (априори-существующая)");
        nextToken();
        //ожидаем открывающую скобку
        expect(TOKEN_TYPE.SEPARATORS, '(');
        //зануляем служебную переменную для хранения параметров функции и считываем их
        currentParameters = new LinkedList<>();
        parameterList();
        //ожидаем закрывающую скобку
        expect(TOKEN_TYPE.SEPARATORS, ')');
        //ожидаем открывающую фигурную скобку
        expect(TOKEN_TYPE.SEPARATORS, '{');
        //зануляем служебную переменную для хранения инструкций и return внутри функции и считываем их
        currentInstructions = new LinkedList<>();
        currentReturnExpression = null;
        instructionList();
        //ожидаем закрывающую фигурную скобку
        expect(TOKEN_TYPE.SEPARATORS, '}');
        //создаем объект-представление для данной функции и добавляем его в список функций
        Type funcType = typeName.equals("void") ? Type.VOID : Type.INTEGER;
        Function f = new Function(funcType, funcName, currentParameters, currentInstructions, currentReturnExpression);
        definedFunctions.add(f);
    }
    private void parameterList() {
        //если ввод начинается с типа, то можем работать, иначе - кидаем исключение
        if (currentToken.getType() == TOKEN_TYPE.KEY_WORD && currentToken.asString().equals("int")) {
            //считываем параметры и пропускаем запятые
            parameter();
            while (currentToken.getType() == TOKEN_TYPE.SEPARATORS && currentToken.asChar() == ',') {
                nextToken();
                parameter();
            }
        } else if (currentToken.getType() == TOKEN_TYPE.KEY_WORD && currentToken.asString().equals("void")) {
            throw new RuntimeException("Тип параметра не может быть void");
        }
    }
    private void parameter() {
        String type = currentToken.asString();
        nextToken();
        //если имя, то создаем объект-представление для параметра
        //и запоминаем в списке параметров текущей функции
        //иначе - кидаем исключение
        if (currentToken.getType() != TOKEN_TYPE.ID)
            throw new RuntimeException("Ожидалось имя параметра, найдено: " + currentToken.getValue());
        String name = currentToken.asString();
        currentParameters.add(new Parameter(type, name));
        nextToken();
    }
    private void instructionList() {
        //пока не встретим закрывающую скобку - считываем инструкции
        //даже при встрече null, несмотря на то, что ошибка тут не вылетит,
        //она вылетит для обработчика объявления функции при вызове expect для закрывающей скобки
        while (currentToken != null && !(currentToken.getType() == TOKEN_TYPE.SEPARATORS && currentToken.asChar() == '}')) {
            if (currentToken.getType() == TOKEN_TYPE.KEY_WORD && currentToken.asString().equals("return")) {
                returnInstruction();
            } else {
                instruction();
            }
        }
    }
    private void instruction() {
        //инструкциями могут быть только вызовы функций
        //поэтому пытаемся обработать вызов функции
        //если все ок- запоминаем в список инструкций текущей функции
        ExpressionFunctionCall call = funcCallExpression();
        currentInstructions.add(ctxExec(call));
        //ожидаем ";" в конце инструкции
        expect(TOKEN_TYPE.SEPARATORS, ';');
    }
    private Instruction ctxExec(ExpressionFunctionCall call) {
        //оборачивает выражение вызова функции в инструкцию
        return new FunctionCallInstruction(call);
    }
    private void returnInstruction() {
        //ожидаем после ключевого слова "return"
        //либо выражение и ";", либо сразу ";"
        //если нашли выражение, то запоминаем в переменную для хранения
        //return-выражение текущей функции
        nextToken();
        if (currentToken.getType() == TOKEN_TYPE.SEPARATORS && currentToken.asChar() == ';') {
            currentReturnExpression = null;
        } else {
            currentReturnExpression = parseExpression();
        }
        expect(TOKEN_TYPE.SEPARATORS, ';');
    }
    private ExpressionFunctionCall funcCallExpression() {
        if (currentToken.getType() != TOKEN_TYPE.ID)
            throw new RuntimeException("Ожидалось имя функции, найдено: " + currentToken.getValue());
        String name = currentToken.asString();
        nextToken();

        expect(TOKEN_TYPE.SEPARATORS, '(');
        //пытаемся считать список выражений, которые будут определять параметры вызова функции
        List<Expression> args = new ArrayList<>();
        if (!(currentToken.getType() == TOKEN_TYPE.SEPARATORS && currentToken.asChar() == ')')) {
            args.add(parseExpression());
            while (currentToken.getType() == TOKEN_TYPE.SEPARATORS && currentToken.asChar() == ',') {
                nextToken();
                args.add(parseExpression());
            }
        }
        expect(TOKEN_TYPE.SEPARATORS, ')');

        return new ExpressionFunctionCall(name, args, Type.INTEGER);
    }
    private Expression parseExpression() {
        //пытаемся понять какое именно выражение перед нами
        //в любом случае выражение начинается с наименьшей единицы (число, идентификатор, вызов функции)
        Expression left = parsePrimary();
        //далее может быть оператор и последующие наименьшие единицы
        while (currentToken != null &&
                currentToken.getType() == TOKEN_TYPE.OPERATORS &&
                (currentToken.asChar() == '+' || currentToken.asChar() == '-')) {

            char op = currentToken.asChar();
            nextToken();
            Expression right = parsePrimary();
            //как бы сливаем два выражения в одно, для последующей
            //композиции со следующими выражениями
            left = new BinaryExpression(left, op, right);
        }
        //получаем единое выражение, являющееся композицией всех наименьших
        return left;
    }
    private Expression parsePrimary() {
        //считывает наименьшее выражение (число или идентификатор или вызов функции)
        if (currentToken.getType() == TOKEN_TYPE.NUMBER) {
            int val = currentToken.asInt();
            nextToken();
            return new NumberExpression(val);
        }
        else if (currentToken.getType() == TOKEN_TYPE.ID) {
            String name = currentToken.asString();
            //пытаемся понять, это просто идентификатор переменной или
            //вызов функции
            //для этого заглядываем на один токен вперед, но не обновляем токен
            //чтобы не потерять скобку если это вызов функции чтобы можно было
            //использовать уже написанную функции парсинга вызова функции
            TOKEN lookahead = scanner.safePeek();
            //если вызов функции - парсим выражение вызова функции
            //иначе - обновляем токен и возвращаем идентификатор
            if (lookahead != null &&
                    lookahead.getType() == TOKEN_TYPE.SEPARATORS &&
                    lookahead.asChar() == '(') {
                return funcCallExpression();
            } else {
                nextToken();
                return new VariableExpression(name, Type.INTEGER);
            }
        }
        else {
            throw new RuntimeException("Ожидалось выражение, найдено: " + currentToken.getValue());
        }
    }
    //----------------- запуск --------------------//
    public void translateAndExecute() {
        //проводим синтаксический разбор
        program();
        //------------------------- семантика -------------------------//
        //формируем глобальный контекст имен функций
        List<Function> allFunctions = new ArrayList<>(existingFunctions);
        allFunctions.addAll(definedFunctions);
        //проверяем все наши функции объявленные на соответствие глобальному контексту
        for (Function f : definedFunctions) {
            f.checkRightContext(allFunctions);
        }
        //формируем глобальный контекст переменных (их нету)
        Map<Parameter, Object> globalContext = new HashMap<>();
        //вызываем все глобальные инструкции отдавая им контекст переменных и функций
        for (Instruction instr : globalInstructions) {
            instr.execute(allFunctions, globalContext);
        }
    }
}
