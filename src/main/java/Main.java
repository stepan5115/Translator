import Utils.Translator;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Running with default test cases...");
            runDefaultTests();
            return;
        }
        String filename = args[0];
        try {
            String fileContent = Files.readString(Paths.get(filename));
            System.out.println("Processing file: " + filename);
            System.out.println("File content:");
            System.out.println(fileContent);
            System.out.println("\nExecution result:");

            Translator translator = new Translator(fileContent);
            translator.translateAndExecute();

        } catch (Exception ex) {
            System.out.println("Error processing file '" + filename + "': " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static void runDefaultTests() {
        String[] tests = {
                """
            int add(int a, int b) { return a + b; }
            int sub(int x, int y) { return x - y; }
            void main() {
            add(5, 3);
            sub(10, 7);
            print( add(1,2) + sub(5,3) );
            print(add(add(add(add(sub(1,2), sub(1, 5)), 1), 1), 1));
            }
            main();
            """,
                "int f(void x) { return x; }",
                "int f(int a) { return b; }",
                "void main() { add(5, 3) }",
                "void f() { return 5; }",
                "int f(int x) { return 10; } f(x" +
                        ");"
        };

        for (int i = 0; i < tests.length; i++) {
            System.out.println("\n=== Test case " + (i + 1) + " ===");
            System.out.println("Code:");
            System.out.println(tests[i]);
            System.out.println("Execution result:");
            try {
                Translator translator = new Translator(tests[i]);
                translator.translateAndExecute();
            } catch (Exception ex) {
                System.out.println("ERROR: " + ex.getMessage());
            }
        }
    }
}
