package com.craftinginterpreters.lox;

import java.util.*;

// import static 语句允许直接访问一个类的静态成员
import static com.craftinginterpreters.lox.TokenType.*;

class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private static final Map<String, TokenType> keywords;
    static {
        keywords = new HashMap<>();
        keywords.put("and",    AND);
        keywords.put("class",  CLASS);
        keywords.put("else",   ELSE);
        keywords.put("false",  FALSE);
        keywords.put("for",    FOR);
        keywords.put("fun",    FUN);
        keywords.put("if",     IF);
        keywords.put("nil",    NIL);
        keywords.put("or",     OR);
        keywords.put("print",  PRINT);
        keywords.put("return", RETURN);
        keywords.put("super",  SUPER);
        keywords.put("this",   THIS);
        keywords.put("true",   TRUE);
        keywords.put("var",    VAR);
        keywords.put("while",  WHILE);
    }

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }
        // not necessary but a litter cleaner
        // EOF: end of line
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
//        System.out.println("char " + c);
        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            case '/':
                if (match('/')) {
                    // A comment goes until the end of the line.
                    // 这里用 peek 而不是用 match, 也是希望能读取到 /n 来处理换行
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                }
                break;
            case ' ':
            case '\r': // 回车符（Carriage Return), 将光标移到当前行的行首，而不向下移动到下一行
            case '\t': // tab
                // Ignore whitespace.
                break;

            case '\n': // Line Feed
                line++;
                break;
            case '"': string(); break;
//            case 'o':
//                if (peek() == 'r') {
//                    addToken(OR);
//                }
//                break;
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    // 保留字
                    // maximal munch
                    identifier();
                } else {
                    // 错误
                    Lox.error(line, "Unexpected character: " + c);
                }
        }
    }

    private void identifier() {
        // maximal munch
        // 如果是字母或数字则继续往下读
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        // 从 keywords 中查找是否是保留字
        TokenType type = keywords.get(text);
        // 不是的话就是一个用户定义的标识符
        if (type == null) type = IDENTIFIER;
        addToken(type);
    }


    private void string() {
        // 支持多行字符串
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        // The closing ".
        advance();

        // Trim the surrounding quotes.
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }


    private void number() {
        while (isDigit(peek())) advance();

        // Look for a fractional part.
        // 如果是小数点，下一个字符还是数字
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the "."
            advance();

            while (isDigit(peek())) advance();
        }
        // start -> current - 1
        addToken(NUMBER,
                Double.parseDouble(source.substring(start, current)));
    }


    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    private char peek() {
        // lookahead
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAtEnd() {
        // 判断是否已消费完所有字符
        return current >= source.length();
    }

    private char advance() {
        current++;
        // charAt(int index) 用于返回指定索引处的字符。
        return source.charAt(current - 1);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}