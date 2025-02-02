package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
    final Environment enclosing; // null if global
    
    // The no-argument constructor is for the global scope’s environment, which ends the chain.
    Environment() {
        enclosing = null;
    }
    
    // The other constructor creates a new local scope nested inside the given outer one.
    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }
    
    // map of variable names to values
    private final Map<String, Object> values = new HashMap<>();

    // 定义变量
    void define(String name, Object value) {
        // 因为新变量总是在当前最内层的作用域中声明
        // 所以不用修改
        values.put(name, value);
        // System.out.println("define " + name + " " + value);
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
        return values.get(name.lexeme);
        }
        // 当前环境中没有找到变量，就在外围环境中尝试，递归该操作
        if (enclosing != null) return enclosing.get(name);

        throw new RuntimeError(name,
            "Undefined variable '" + name.lexeme + "'.");
    }

    // assign 是赋值
    // The key difference between assignment and definition is that assignment is not allowed to create a new variable.
    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            // System.out.println("assign " + name.lexeme + " " + value);
            values.put(name.lexeme, value);
            return;
        }
        // 不会被覆盖么？是会覆盖最近一个父环境的同名变量，如果当前 block 没有先 define 一个同名变量的话
        // 递归检查外部环境
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name,
            "Undefined variable '" + name.lexeme + "'.");
    }
}