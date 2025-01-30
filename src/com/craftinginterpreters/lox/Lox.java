package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    static boolean hadError = false;

    private static void run(String source) {
//        System.out.println(source);
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        // For now, just print the tokens.
        for (Token token : tokens) {
            System.out.println(token);
        }
    }

    private static void runFile(String path) throws IOException {
        // Paths.get(path) 将字符串形式的文件路径转换为 Path 对象
        // 使用上述 Path 对象读取文件的所有字节到一个字节数组 bytes 中。这个方法尝试一次性读取整个文件，所以适用于读取小到中等大小的文件。
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        // 将字节数组 bytes 转换为一个新的字符串。
        run(new String(bytes, Charset.defaultCharset()));
        // Indicate an error in the exit code.
        if (hadError) System.exit(65);
    }

    private static void runPrompt() throws IOException {
        // 创建了一个 InputStreamReader 对象，它将字节流（来自标凈输入 System.in）转换成字符流。
        InputStreamReader input = new InputStreamReader(System.in);
        // 它使用缓冲来读取字符输入流，这样可以一次读取一行文本，提高效率。
        BufferedReader reader = new BufferedReader(input);

        // 无限循环，通常也可以写成 while (true)
        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
//            System.out.println(line);
            // 当从标准输入读取到文件结束符（EOF）时，readLine() 会返回 null
            // 这通常意味着没有更多的输入可以读取（比如用户按下了 Ctrl+D），于是退出循环。
            if (line == null) break;
            run(line);
            hadError = false;
        }
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.println(
                "[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            System.out.println("File path: " + args[0]);
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }
}