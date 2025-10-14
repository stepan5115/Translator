package Entities;

import java.util.List;
import java.util.Map;

public abstract class Instruction {
    public abstract void execute(List<Function> function, Map<Parameter, Object> context);
}
