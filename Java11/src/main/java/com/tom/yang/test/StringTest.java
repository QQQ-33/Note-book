package com.tom.yang.test;

import org.junit.Test;

import java.util.stream.Stream;

/**
 * @author qk965
 * @date 2021-07-15 11:45
 */
public class StringTest {

    @Test
    public void test(){
        // 判断字符串是否为空白
        System.out.println(" ".isBlank());
        // 去除首尾空格
        System.out.println(" Hello World ".strip());
        System.out.println(" Hello World ".stripTrailing());
        System.out.println(" Hello World ".stripLeading());
        // 重复并返回新字串
        String s = "abc".repeat(3);
        System.out.println(s);
        // 行分析
        Stream<String> lines = " a \n b \n c \n".lines();
        System.out.println(lines.count());
    }
}
