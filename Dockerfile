FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

# Компилируем ВСЕ Java файлы
RUN find . -name "*.java" > sources.txt && javac -d bin @sources.txt

# Или явно перечисляем все основные классы
RUN javac -d bin \
    src/main/java/Entities/Expression.java \
    src/main/java/Entities/Type.java \
    src/main/java/Entities/Parameter.java \
    src/main/java/Entities/NumberExpression.java \
    src/main/java/Entities/VariableExpression.java \
    src/main/java/Entities/BinaryExpression.java \
    src/main/java/Entities/ExpressionFunctionCall.java \
    src/main/java/Entities/Function.java \
    src/main/java/Entities/Instruction.java \
    src/main/java/Entities/FunctionCallInstruction.java \
    src/main/java/Utils/Scanner.java \
    src/main/java/Utils/Translator.java \
    src/main/java/Main.java

ENTRYPOINT ["java", "-cp", "bin", "Main"]
CMD []