package com.tom.yang.test;

import org.junit.Test;

import java.util.Optional;

/**
 * @author qk965
 * @date 2021-07-15 12:03
 */
public class OptionalTest {

    @Test
    public void test(){
        // optional 的值返回 stream
        Optional.of("Java 11").stream().forEach(System.out::println);
        Optional.ofNullable(null).stream().forEach(System.out::println);

//        Optional<Integer> op = Optional.of(1);
        Optional<Integer> op = Optional.ofNullable(null);
        // 对 ifPresent 的增强
        op.ifPresentOrElse(
                x -> System.out.println(x),
                () -> System.out.println("Not present!"));
    }
}
