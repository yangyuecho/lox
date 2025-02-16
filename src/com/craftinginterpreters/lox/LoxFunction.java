package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final Stmt.Function declaration;
  private final Environment closure;


  LoxFunction(Stmt.Function declaration, Environment closure) {
    this.closure = closure;
    this.declaration = declaration;
  }

  @Override
  public Object call(Interpreter interpreter,
                     List<Object> arguments) {
    // Environment environment = new Environment(interpreter.globals);
    // 每个函数有自己的环境
    Environment environment = new Environment(closure);
    for (int i = 0; i < declaration.params.size(); i++) {
      environment.define(declaration.params.get(i).lexeme,
          arguments.get(i));
    }

    // 当捕获一个返回异常时，它会取出其中的值并将其作为call()方法的返回值。
    // 如果没有捕获任何异常，意味着函数到达了函数体的末尾，而且没有遇到return语句。
    // 在这种情况下，隐式地返回nil。
    try {
        interpreter.executeBlock(declaration.body, environment);
    } catch (Return returnValue) {
        return returnValue.value;
    }
    return null;
  }

  @Override
  public int arity() {
    return declaration.params.size();
  }

  @Override
  public String toString() {
    return "<fn " + declaration.name.lexeme + ">";
  }
}