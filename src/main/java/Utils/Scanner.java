package Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

public final class Scanner {
    private Character bufferCharacter = null;
    private InputStreamReader reader = new InputStreamReader(System.in);
    private TOKEN peekedToken = null;
    private final static String[] KEYWORDS = { "void", "int", "return" };
    private static final Set<Character> SEPARATORS = Set.of('(', ')', '{', '}', ';', ',');
    private static final Set<Character> OPERATORS = Set.of('+', '-');

    public Scanner() {}
    public Scanner(InputStream inputStream) { setInputStream(inputStream); }

    public void setInputStream(InputStream inputStream) {
        reader = new InputStreamReader(inputStream);
    }

    public enum TOKEN_TYPE {
        ID, NUMBER, KEY_WORD, SEPARATORS, OPERATORS, NOT_DEFINED
    }

    public static class TOKEN {
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

    public TOKEN nextToken() throws IOException {
        if (peekedToken != null) {
            TOKEN result = peekedToken;
            peekedToken = null;
            return result;
        }
        return nextTokenInternal();
    }

    public TOKEN peekNextToken() throws IOException {
        if (peekedToken == null) {
            peekedToken = nextTokenInternal();
        }
        return peekedToken;
    }

    public TOKEN safePeek() {
        try {
            return peekNextToken();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при просмотре следующего токена: " + e.getMessage(), e);
        }
    }


    private TOKEN nextTokenInternal() throws IOException  {
        StringBuilder result = new StringBuilder();
        TOKEN_TYPE currentType = TOKEN_TYPE.NOT_DEFINED;
        int data;
        while (true) {
            char current;
            if (bufferCharacter != null) {
                current = bufferCharacter;
                bufferCharacter = null;
            } else {
                data = reader.read();
                if (data == -1) break;
                current = (char)data;
            }
            if (Character.isWhitespace(current)) {
                if (currentType != TOKEN_TYPE.NOT_DEFINED) {
                    return createToken(result.toString(), currentType);
                } else {
                    continue;
                }
            }
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
        if (result.isEmpty())
            return null;
        return createToken(result.toString(), currentType);
    }

    private TOKEN createToken(String result, TOKEN_TYPE currentType) {
        switch (currentType) {
            case ID:
                for (String keyword : KEYWORDS)
                    if (keyword.equals(result)) {
                        currentType = TOKEN_TYPE.KEY_WORD;
                        break;
                    }
                return new TOKEN(currentType, result);
            case NUMBER:
                if ((result.length() > 1) && (result.charAt(0) == '0')) {
                    currentType = TOKEN_TYPE.NOT_DEFINED;
                    return new TOKEN(currentType, result);
                }
                return new TOKEN(currentType, Integer.parseInt(result));
            default:
                return new TOKEN(TOKEN_TYPE.NOT_DEFINED, result);
        }
    }
}
