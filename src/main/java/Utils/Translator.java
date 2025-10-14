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
    private void nextToken() {
        try {
            currentToken = scanner.nextToken();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка чтения токена: " + e.getMessage());
        }
    }
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
        if (currentToken.getType() == TOKEN_TYPE.KEY_WORD &&
                (currentToken.asString().equals("int") || currentToken.asString().equals("void"))) {
            funcDeclaration();
        } else if (currentToken.getType() == TOKEN_TYPE.ID) {
            ExpressionFunctionCall call = funcCallExpression();
            globalInstructions.add(ctxExec(call));
            expect(TOKEN_TYPE.SEPARATORS, ';');
        } else {
            throw new RuntimeException("Ошибка в элементе программы: " + currentToken.getValue());
        }
    }
    private void funcDeclaration() {
        String typeName = currentToken.asString();
        nextToken();

        if (currentToken.getType() != TOKEN_TYPE.ID)
            throw new RuntimeException("Ожидалось имя функции, найдено: " + currentToken.getValue());
        String funcName = currentToken.asString();
        for (Function function : definedFunctions)
            if (function.getName().equals(funcName))
                throw new RuntimeException("Функция " + funcName + " уже объявлена");
        for (Function function : existingFunctions)
            if (function.getName().equals(funcName))
                throw new RuntimeException("Функция " + funcName + " уже существует (априори-существующая)");
        nextToken();

        expect(TOKEN_TYPE.SEPARATORS, '(');
        currentParameters = new LinkedList<>();
        parameterList();
        expect(TOKEN_TYPE.SEPARATORS, ')');

        expect(TOKEN_TYPE.SEPARATORS, '{');
        currentInstructions = new LinkedList<>();
        currentReturnExpression = null;
        instructionList();
        expect(TOKEN_TYPE.SEPARATORS, '}');

        Type funcType = typeName.equals("void") ? Type.VOID : Type.INTEGER;
        Function f = new Function(funcType, funcName, currentParameters, currentInstructions, currentReturnExpression);
        definedFunctions.add(f);
    }
    private void parameterList() {
        if (currentToken.getType() == TOKEN_TYPE.KEY_WORD && currentToken.asString().equals("int")) {
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
        if (currentToken.getType() != TOKEN_TYPE.ID)
            throw new RuntimeException("Ожидалось имя параметра, найдено: " + currentToken.getValue());
        String name = currentToken.asString();
        currentParameters.add(new Parameter(type, name));
        nextToken();
    }
    private void instructionList() {
        while (currentToken != null && !(currentToken.getType() == TOKEN_TYPE.SEPARATORS && currentToken.asChar() == '}')) {
            if (currentToken.getType() == TOKEN_TYPE.KEY_WORD && currentToken.asString().equals("return")) {
                returnInstruction();
            } else {
                instruction();
            }
        }
    }
    private void instruction() {
        ExpressionFunctionCall call = funcCallExpression();
        currentInstructions.add(ctxExec(call));
        expect(TOKEN_TYPE.SEPARATORS, ';');
    }
    private Instruction ctxExec(ExpressionFunctionCall call) {
        return new FunctionCallInstruction(call);
    }
    private void returnInstruction() {
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
        Expression left = parsePrimary();

        while (currentToken != null &&
                currentToken.getType() == TOKEN_TYPE.OPERATORS &&
                (currentToken.asChar() == '+' || currentToken.asChar() == '-')) {

            char op = currentToken.asChar();
            nextToken();
            Expression right = parsePrimary();

            left = new BinaryExpression(left, op, right);
        }
        return left;
    }
    private Expression parsePrimary() {
        if (currentToken.getType() == TOKEN_TYPE.NUMBER) {
            int val = currentToken.asInt();
            nextToken();
            return new NumberExpression(val);
        }
        else if (currentToken.getType() == TOKEN_TYPE.ID) {
            String name = currentToken.asString();
            TOKEN lookahead = scanner.safePeek();
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
    private void funcCall() {
        if (currentToken.getType() != TOKEN_TYPE.ID)
            throw new RuntimeException("Ожидалось имя функции, найдено: " + currentToken.getValue());
        String name = currentToken.asString();
        nextToken();

        expect(TOKEN_TYPE.SEPARATORS, '(');
        argumentList();
        expect(TOKEN_TYPE.SEPARATORS, ')');

        System.out.println("Вызов функции: " + name);
    }
    private void argumentList() {
        if (currentToken.getType() == TOKEN_TYPE.ID || currentToken.getType() == TOKEN_TYPE.NUMBER) {
            expression();
            argumentListPrime();
        }
    }
    private void argumentListPrime() {
        while (currentToken.getType() == TOKEN_TYPE.SEPARATORS && currentToken.asChar() == ',') {
            nextToken();
            expression();
        }
    }
    private void expression() {
        term();
        while (currentToken != null && currentToken.getType() == TOKEN_TYPE.OPERATORS
                && ((currentToken.asChar() == '+') || currentToken.asChar() == '-')) {
            String op = currentToken.asString();
            nextToken();
            term();
            System.out.println("Операция: " + op);
        }
    }
    private void term() {
        try {
            if (currentToken.getType() == TOKEN_TYPE.ID) {
                TOKEN lookahead = scanner.peekNextToken();
                if (lookahead != null && lookahead.getType() == TOKEN_TYPE.SEPARATORS && lookahead.asChar() == '(') {
                    funcCall();
                } else {
                    nextToken();
                }
            } else if (currentToken.getType() == TOKEN_TYPE.NUMBER) {
                nextToken();
            } else {
                throw new RuntimeException("Ожидалось выражение, найдено: " + currentToken.getValue());
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при разборе выражения: " + e.getMessage(), e);
        }
    }


    //----------------- запуск --------------------//
    public void translateAndExecute() {
        program();

        List<Function> allFunctions = new ArrayList<>(existingFunctions);
        allFunctions.addAll(definedFunctions);

        for (Function f : definedFunctions) {
            f.checkRightContext(allFunctions);
        }

        Map<Parameter, Object> globalContext = new HashMap<>();

        for (Instruction instr : globalInstructions) {
            instr.execute(allFunctions, globalContext);
        }
    }
}
