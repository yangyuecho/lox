package com.craftinginterpreters.lox;

class Token {
    final TokenType type;
    final String lexeme;  // 词素
    final Object literal;  // 文字 ？
    final int line;

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}