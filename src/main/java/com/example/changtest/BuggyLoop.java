/**
 * Copyright 2025 铁路12306科创中心
 *
 * @author changjiasheng
 * @date 2025年11月28日
 */
package com.example.changtest;

import java.util.ArrayList;
import java.util.List;

public class BuggyLoop {
    private static List<byte[]> memoryLeakList = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("开始执行有问题的循环...");

        // 外层死循环 - 导致高CPU使用
        while (true) {
            // 内存泄漏：不断向列表添加数据，但从不清理
            byte[] data = new byte[1024 * 1024]; // 每次分配1MB
            memoryLeakList.add(data);

            // 高CPU使用：不必要的复杂计算
            highCpuCalculation();

            // 短暂休眠，但不足以缓解问题
            try {
                Thread.sleep(10); // 10毫秒，CPU仍然很高
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 每100次循环打印一次状态
            if (memoryLeakList.size() % 100 == 0) {
                System.out.println("已分配内存: " + memoryLeakList.size() + " MB");
            }
        }
    }

    // 高CPU使用的方法
    private static void highCpuCalculation() {
        double result = 0;
        // 不必要的密集计算
        for (int i = 0; i < 100000; i++) {
            result += Math.sin(i) * Math.cos(i) / Math.sqrt(i + 1);
        }
    }

    // 永远不会被调用的清理方法（故意设计）
    @SuppressWarnings("unused")
    private static void cleanup() {
        memoryLeakList.clear();
        System.gc();
    }
}
