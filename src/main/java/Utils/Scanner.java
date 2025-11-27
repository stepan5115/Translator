package Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

public final class Scanner {
    //хранит считанный из потока ввода символ
    private Character bufferCharacter = null;
    //поток ввода
    private InputStreamReader reader = new InputStreamReader(System.in);
    //помогает реализовать возможность для внешних пользователей считывать следующий токен, но не обновлять его
    private TOKEN peekedToken = null;
    //служебные массивы
    private final static String[] KEYWORDS = { "void", "int", "return" };
    private static final Set<Character> SEPARATORS = Set.of('(', ')', '{', '}', ';', ',');
    private static final Set<Character> OPERATORS = Set.of('+', '-');

    //пара конструкторов
    public Scanner() {}
    public Scanner(InputStream inputStream) { setInputStream(inputStream); }
    //возможность сменить поток ввода
    public void setInputStream(InputStream inputStream) {
        reader = new InputStreamReader(inputStream);
    }
    //типы токенов
    public enum TOKEN_TYPE {
        ID, NUMBER, KEY_WORD, SEPARATORS, OPERATORS, NOT_DEFINED
    }
    //класс токенов
    public static class TOKEN {
        //создание и получение информации из токена
        private final TOKEN_TYPE type;
        private final Object value;

        public TOKEN(TOKEN_TYPE type, Object value) {
            this.type = type;
            this.value = value;
        }

        public TOKEN_TYPE getType() {
            return type;
        }

        public Object getValue() {
            return value;
        }
        //различные преобразования в требуемые классы
        public String asString() {
            if (value instanceof String) return (String) value;
            if (value instanceof Character) return value.toString();
            throw new ClassCastException("Токен не конвертируется в String: " + value.getClass());
        }

        public char asChar() {
            if (value instanceof Character) return (Character) value;
            if (value instanceof String && ((String) value).length() == 1) return ((String) value).charAt(0);
            throw new ClassCastException("Токен не конвертируется в Char: " + value.getClass());
        }

        public int asInt() {
            if (value instanceof Integer) return (Integer) value;
            throw new ClassCastException("Токен не конвертируется в int: " + value.getClass());
        }
    }
    //следующий токен (взять и обновить на новый)
    public TOKEN nextToken() throws IOException {
        //если есть уже буферизированный токен, полученный в результате
        //просмотра без обновления, то возвращаем сразу его
        if (peekedToken != null) {
            TOKEN result = peekedToken;
            peekedToken = null;
            return result;
        }
        //иначе, считываем и возвращаем токен
        return nextTokenInternal();
    }
    //позволяет получить токен, без его обновления на следующий
    public TOKEN peekNextToken() throws IOException {
        //если не заглядывали ранее, то считываем токен и буферизируем его
        if (peekedToken == null) {
            peekedToken = nextTokenInternal();
        }
        //если уже заглядывали, то возвращаем что считали
        return peekedToken;
    }
    //аналог peekNextToken(), но в силу особенностей Java
    //он потребовался чтобы в его сигнатуре не было throws IOException, как у peekNextToken()
    public TOKEN safePeek() {
        try {
            return peekNextToken();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при просмотре следующего токена: " + e.getMessage(), e);
        }
    }
    //Считывание нового токена
    private TOKEN nextTokenInternal() throws IOException  {
        StringBuilder result = new StringBuilder();
        TOKEN_TYPE currentType = TOKEN_TYPE.NOT_DEFINED;
        int data;
        //считываем по символам
        while (true) {
            char current;
            //если есть буферизированный символ, берем его. Иначе - считваем новый
            if (bufferCharacter != null) {
                current = bufferCharacter;
                bufferCharacter = null;
            } else {
                data = reader.read();
                if (data == -1) break;
                current = (char)data;
            }
            //если знаки табуляции какие-либо, то заканчиваем разбор
            //в случае если у нас уже определился какой-либо токен,
            // иначе - пропускаем символ и повторяем цикл для нового
            // (вдруг у нас перед токеном много знаков табуляции)
            if (Character.isWhitespace(current)) {
                if (currentType != TOKEN_TYPE.NOT_DEFINED) {
                    return createToken(result.toString(), currentType);
                } else {
                    continue;
                }
            }
            //если тип еще не определился, то пытаемся по
            //символу выяснить какой тип (выполняется при первом символе,
            //отличном от знаков табуляции)
            if (currentType == TOKEN_TYPE.NOT_DEFINED) {
                result.append(current);
                if (Character.isLetter(current))
                    currentType = TOKEN_TYPE.ID;
                else if (Character.isDigit(current))
                    currentType = TOKEN_TYPE.NUMBER;
                else if (SEPARATORS.contains(current)) {
                    currentType = TOKEN_TYPE.SEPARATORS;
                    return new TOKEN(currentType, current);
                }
                else if (OPERATORS.contains(current)) {
                    currentType = TOKEN_TYPE.OPERATORS;
                    return new TOKEN(currentType, current);
                }
                else
                    return new TOKEN(currentType, String.valueOf(current));
            } else {
            //если у нас уже определен тип, то пытаемся дополнить значение токена новым символом
            //если считанный символ не подходит в определенный тип токена, то буферищируем его
            //и возвращаем немного обработанный aeyrwbtq createToken() токен
                switch (currentType) {
                    case ID:
                        if (Character.isLetter(current) || Character.isDigit(current))
                            result.append(current);
                        else {
                            bufferCharacter = current;
                            return createToken(result.toString(), currentType);
                        }
                        break;
                    case NUMBER:
                        if (Character.isDigit(current))
                            result.append(current);
                        else {
                            bufferCharacter = current;
                            return createToken(result.toString(), currentType);
                        }
                        break;
                }
            }
        }
        //если вышли из цикла (скорее всего поток ввода закончился)
        //то если ничего не считали, то - null. Иначе - возвращаем токен который
        //получили на моменте выхода немного обработав функцией createToken()
        if (result.isEmpty())
            return null;
        return createToken(result.toString(), currentType);
    }
    //обрабатывает невалидные значения для токенов типа ID и NUMBER
    private TOKEN createToken(String result, TOKEN_TYPE currentType) {
        switch (currentType) {
            case ID:
                //если ID совпадает с ключевым словом, то надо поменять тип токена
                for (String keyword : KEYWORDS)
                    if (keyword.equals(result)) {
                        currentType = TOKEN_TYPE.KEY_WORD;
                        break;
                    }
                return new TOKEN(currentType, result);
            case NUMBER:
                //если число начинается с 0, то это не число - меняем тип
                if ((result.length() > 1) && (result.charAt(0) == '0')) {
                    currentType = TOKEN_TYPE.NOT_DEFINED;
                    return new TOKEN(currentType, result);
                }
                return new TOKEN(currentType, Integer.parseInt(result));
            default:
                //другие типы мы никак не обрабатываем
                return new TOKEN(TOKEN_TYPE.NOT_DEFINED, result);
        }
    }
}
