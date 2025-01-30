package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

/* (chapter 8 - 2)
program        → declaration* EOF ;

declaration    → varDecl
               | statement ;

statement      → exprStmt
               | printStmt
               | block ;

block          → "{" declaration* "}" ;


varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
primary        → "true" | "false" | "nil"
               | NUMBER | STRING
               | "(" expression ")"
               | IDENTIFIER ;
*/

/* (chapter 8)
program        → statement* EOF ;

statement      → exprStmt
               | printStmt ;

exprStmt       → expression ";" ;
printStmt      → "print" expression ";" ;
*/

/* only support expression (chapter 6)
// Since equality has the lowest precedence, if we match that, then it covers everything.
// expression     → equality ; (废弃)

expression     → assignment ;
assignment     → IDENTIFIER "=" assignment
               | equality ;
// precedence rules as C, going from lowest to highest. 优先级
// 此处的每个规则仅匹配其当前优先级或更高优先级的表达式
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;
unary          → ( "!" | "-" ) unary
               | primary ;
// all the literals and grouping expressions.
primary        → NUMBER | STRING | "true" | "false" | "nil"
                | "(" expression ")" ;
                | IDENTIFIER ;
*/
public class Parser {
    //  a simple sentinel class
    private static class ParseError extends RuntimeException {}

    // like the scanner, the parser consumers a flat input sequence
    // reading tokens instead of characters
    private final List<Token> tokens;
    private int current = 0;
    
    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // Expr parse() {
    //     try {
    //         return expression();
    //     } catch (ParseError error) {
    //         return null;
    //     }
    // }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
          statements.add(declaration());
        }
    
        return statements; 
    }

    // 执行表达式
    private Expr expression() {
        // return equality();
        return assignment();
    }

    private Stmt declaration() {
        try {
          if (match(VAR)) return varDeclaration();
    
          return statement();
        } catch (ParseError error) {
          synchronize();
          return null;
        }
    }

    private Stmt statement() {
        if (match(PRINT)) return printStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");
    
        Expr initializer = null;
        if (match(EQUAL)) {
          initializer = expression();
        }
    
        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private List<Stmt> block() {
      List<Stmt> statements = new ArrayList<>();
  
      while (!check(RIGHT_BRACE) && !isAtEnd()) {
        statements.add(declaration());
      }
  
      consume(RIGHT_BRACE, "Expect '}' after block.");
      return statements;
    }

    private Expr assignment() {
      Expr expr = equality();
  
      if (match(EQUAL)) {
        // 左值不用求值
        Token equals = previous();
        Expr value = assignment();
  
        if (expr instanceof Expr.Variable) {
          Token name = ((Expr.Variable)expr).name;
          return new Expr.Assign(name, value);
        }
  
        error(equals, "Invalid assignment target."); 
      }
  
      return expr;
    }

    // 可变参数，在方法内部，types 会被当作一个 TokenType[] 数组来处理。
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
          if (check(type)) {
            advance();
            return true;
          }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }
    
    private Token peek() {
        return tokens.get(current);
    }
    
    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    // 在捕获一个 ParseError 之后调用，回到同步状态
    // 丢弃可能回引起级联错误的语法标记，从下一条语句开始解析文件的其余部分
    private void synchronize() {
        advance();
    
        // 找到下一条语句的开头
        while (!isAtEnd()) {
          if (previous().type == SEMICOLON) return;
    
          switch (peek().type) {
            case CLASS:
            case FUN:
            case VAR:
            case FOR:
            case IF:
            case WHILE:
            case PRINT:
            case RETURN:
                return;
          }
    
          advance();
        }
      }


    // equality  → comparison ( ( "!=" | "==" ) comparison )* ;
    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // comparison   → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }
    
    private Expr term() {
        Expr expr = factor();
    
        while (match(MINUS, PLUS)) {
          Token operator = previous();
          Expr right = factor();
          expr = new Expr.Binary(expr, operator, right);
        }
    
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();
    
        while (match(SLASH, STAR)) {
          Token operator = previous();
          Expr right = unary();
          expr = new Expr.Binary(expr, operator, right);
        }
    
        return expr;
    }

    // unary   → ( "!" | "-" ) unary | primary ;
    private Expr unary() {
        if (match(BANG, MINUS)) {
          Token operator = previous();
          Expr right = unary();
          return new Expr.Unary(operator, right);
        }
    
        return primary();
    }

    // primary   → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" ;
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);
    
        if (match(NUMBER, STRING)) {
          return new Expr.Literal(previous().literal);
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
    
        if (match(LEFT_PAREN)) {
          Expr expr = expression();
          consume(RIGHT_PAREN, "Expect ')' after expression.");
          return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }
}
