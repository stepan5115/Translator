package Entities;

//класс для представления параметров функции
public class Parameter {
    private final Type type;
    private final String name;

    public Parameter(String type, String name) {
        if (!type.equals("int"))
            throw new RuntimeException("Тип параметра void или не существует, а так нельзя!");
        this.type = Type.INTEGER;
        this.name = name;
    }

    public Type getType() {
        return type;
    }
    public String getName() {
        return name;
    }
}
