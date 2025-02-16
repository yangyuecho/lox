package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

/* (chapter 8 - 2)
program        → declaration* EOF ;

declaration    → funDecl
               | varDecl
               | statement ;

funDecl        → "fun" function ;
function       → IDENTIFIER "(" parameters? ")" block ;
parameters     → IDENTIFIER ( "," IDENTIFIER )* ;

statement      → exprStmt
               | forStmt
               | ifStmt
               | printStmt
               | returnStmt
               | whileStmt
               | block ;

returnStmt     → "return" expression? ";" ;

forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
                 expression? ";"
                 expression? ")" statement ;

whileStmt      → "while" "(" expression ")" statement ;

ifStmt         → "if" "(" expression ")" statement  (chapter 9)
               ( "else" statement )? ;

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
               | logic_or ;
logic_or       → logic_and ( "or" logic_and )* ;  (chapter 9)
logic_and      → equality ( "and" equality )* ;  (chapter 9)
// precedence rules as C, going from lowest to highest. 优先级
// 此处的每个规则仅匹配其当前优先级或更高优先级的表达式
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;
unary          → ( "!" | "-" ) unary | call ;
call           → primary ( "(" arguments? ")" )* ;
arguments      → expression ( "," expression )* ;
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
          if (match(FUN)) return function("function");
          if (match(VAR)) return varDeclaration();
          return statement();
        } catch (ParseError error) {
          synchronize();
          return null;
        }
    }

    private Stmt statement() {
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(RETURN)) return returnStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        if (match(WHILE)) return whileStatement();

        return expressionStatement();
    }

    private Stmt returnStatement() {
      Token keyword = previous();
      Expr value = null;
      if (!check(SEMICOLON)) {
        value = expression();
      }
  
      consume(SEMICOLON, "Expect ';' after return value.");
      return new Stmt.Return(keyword, value);
    }

    // 语法糖, 脱糖
    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");
        // 初始化式
        // 如果(后面的标记是分号，那么初始化式就被省略了。
        // 否则，我们就检查var关键字，看它是否是一个变量声明。如果这两者都不符合，那么它一定是一个表达式
        Stmt initializer;
        if (match(SEMICOLON)) {
          initializer = null;
        } else if (match(VAR)) {
          initializer = varDeclaration();
        } else {
          initializer = expressionStatement();
        }
        // 条件语句
        Expr condition = null;
        if (!check(SEMICOLON)) {
          condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");
        // 增量语句 i++
        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
          increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");
        // 循环主体
        Stmt body = statement();
        if (increment != null) {
          // 后面执行增量子语句
          body = new Stmt.Block(
              Arrays.asList(
                  body,
                  new Stmt.Expression(increment)));
        }
        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);
        // 最后，如果有初始化式，它会在整个循环之前运行一次。
        // 我们的做法是，再次用代码块来替换整个语句，该代码块中首先运行一个初始化式，然后执行循环。
        if (initializer != null) {
          body = new Stmt.Block(Arrays.asList(initializer, body));
        }
    
        return body;
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt ifStatement() {
      consume(LEFT_PAREN, "Expect '(' after 'if'.");
      Expr condition = expression();
      consume(RIGHT_PAREN, "Expect ')' after if condition."); 
  
      Stmt thenBranch = statement();
      Stmt elseBranch = null;
      // else与前面最近的if绑定在一起
      if (match(ELSE)) {
        // 因为 ifStatement()在返回之前会继续寻找一个else子句，
        // 连续嵌套的最内层调用在返回外部的if语句之前，会先为自己声明else语句。
        elseBranch = statement();
      }
  
      return new Stmt.If(condition, thenBranch, elseBranch);
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

    private Stmt whileStatement() {
      consume(LEFT_PAREN, "Expect '(' after 'while'.");
      Expr condition = expression();
      consume(RIGHT_PAREN, "Expect ')' after condition.");
      Stmt body = statement();
  
      return new Stmt.While(condition, body);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private Stmt.Function function(String kind) {
        Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
        consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
          do {
            if (parameters.size() >= 255) {
              error(peek(), "Can't have more than 255 parameters.");
            }

            parameters.add(
                consume(IDENTIFIER, "Expect parameter name."));
          } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.");

        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
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
      Expr expr = or();
  
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

    private Expr or() {
      Expr expr = and();
  
      while (match(OR)) {
        Token operator = previous();
        Expr right = and();
        expr = new Expr.Logical(expr, operator, right);
      }
  
      return expr;
    }

    private Expr and() {
      Expr expr = equality();
  
      while (match(AND)) {
        Token operator = previous();
        Expr right = equality();
        expr = new Expr.Logical(expr, operator, right);
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
    
        return call();
    }

    // call           → primary ( "(" arguments? ")" )* ;
    private Expr call() {
      Expr expr = primary();
  
      while (true) { 
        if (match(LEFT_PAREN)) {
          expr = finishCall(expr);
        } else {
          break;
        }
      }
  
      return expr;
    }

    private Expr finishCall(Expr callee) {
      List<Expr> arguments = new ArrayList<>();
      // check if the zero-argument case
      if (!check(RIGHT_PAREN)) {
        do {
          // Java规范规定一个方法可以接受不超过255个参数。
          // 这里和 java 保持一致
          if (arguments.size() >= 255) {
            error(peek(), "Can't have more than 255 arguments.");
          }
          arguments.add(expression());
        } while (match(COMMA));
      }
  
      Token paren = consume(RIGHT_PAREN,
                            "Expect ')' after arguments.");
  
      return new Expr.Call(callee, paren, arguments);
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
