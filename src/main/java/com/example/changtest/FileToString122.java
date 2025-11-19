/**
 * Copyright 2025 铁路12306科创中心
 *
 * @author changjiasheng
 * @date 2025年11月18日
 */
package com.example.changtest;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FileToString122 {

    public static void main(String[] args) {
        // 文件路径（替换为你的文件路径）
        String filePath = "test.txt";
        // 用于拼接文件内容的字符串缓冲区（效率高于 String 直接拼接）
        StringBuilder content = new StringBuilder();

        // try-with-resources 语法：自动关闭流，无需手动调用 close()
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            // 循环读取每一行，直到 null（文件末尾）
            while ((line = br.readLine()) != null) {
                // 拼接当前行内容，若需要保留换行符可加 "\n"
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            // 处理文件读取异常（如文件不存在、权限问题等）
            e.printStackTrace();
        }

        // 转换为字符串并输出
        String result = content.toString();
        System.out.println("文件内容：");
        System.out.println(result);
    }
}
