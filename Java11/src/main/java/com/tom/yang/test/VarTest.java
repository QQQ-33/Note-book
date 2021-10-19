package com.tom.yang.test;

import org.junit.Test;

/**
 * @author qk965
 * @date 2021-07-15 10:44
 */
public class VarTest {

    /**
     * var 自动推断类型
     * 但它不是关键字，编译时会被改写为确定的类型
     * 不能用于类变量
     */
    @Test
    public void test(){
        var a = 1;
        System.out.println(a);
        var str = "test var";
        System.out.println(str);
    }


}
