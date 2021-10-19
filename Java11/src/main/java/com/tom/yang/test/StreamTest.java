package com.tom.yang.test;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * @author qk965
 * @date 2021-07-15 11:57
 */
public class StreamTest {
    Integer[] arr;

    @Before
    public void pre(){
        arr = new Integer[]{6, 10, 11, 15, 20};
    }

    @Test
    public void test(){
        // 创建一个空流
        Stream<Object> objectStream = Stream.ofNullable(null);
//        System.out.println(objectStream);

        // 判断如果为true就 取出 来生成一个新的流,只要碰到false就终止
        Arrays.stream(arr).takeWhile( a -> (a % 2) == 0).forEach(System.out::println);
        // 判断如果为true就 丢弃 来生成一个新的流,只要碰到false就终止
        Arrays.stream(arr).dropWhile( a -> (a % 2) == 0).forEach(System.out::println);
    }
}
