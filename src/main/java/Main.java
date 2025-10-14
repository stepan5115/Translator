import Utils.Translator;

public class Main {
    public static void main(String[] args) {
        String test = """
                int add(int a, int b) { return a + b; }
                int sub(int x, int y) { return x - y; }
                void main() {
                add(5, 3);
                sub(10, 7);
                print( add(1,2) + sub(5,3) );
                print(add(add(add(add(sub(1,2), sub(1, 5)), 1), 1), 1));
                }
                main();
                """;
        String test1 = "int f(void x) { return x; }";
        String test2 = "int f(int a) { return b; }";
        String test3 = "void main() { add(5, 3) }";
        String test4 = "void f() { return 5; }";
        try {
            Translator translator = new Translator(test);
            translator.translateAndExecute();
        } catch (Exception ex) {
            System.out.println(ex);
        }
        try {
            Translator translator = new Translator(test1);
            translator.translateAndExecute();
        } catch (Exception ex) {
            System.out.println(ex);
        }
        try {
            Translator translator = new Translator(test2);
            translator.translateAndExecute();
        } catch (Exception ex) {
            System.out.println(ex);
        }
        try {
            Translator translator = new Translator(test3);
            translator.translateAndExecute();
        } catch (Exception ex) {
            System.out.println(ex);
        }
        try {
            Translator translator = new Translator(test4);
            translator.translateAndExecute();
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }
}
