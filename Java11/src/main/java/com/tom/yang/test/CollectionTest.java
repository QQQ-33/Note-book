package com.tom.yang.test;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author qk965
 * @date 2021-07-15 11:37
 */
public class CollectionTest {
    /**
     * 集合增强
     * of 创建不可变集合，不能进行添加、删除、替换、排序
     * copyOf 创建不可变集合，如果以不可变集合创建则直接返回
     */
    @Test
    public void test(){
        var list = List.of(1, 2, 3);
        // list.add(1); // java.lang.UnsupportedOperationException
        var copyList = List.copyOf(list);
        System.out.println(list == copyList);
        list = new ArrayList<>();
        System.out.println(list == copyList);
    }

    @Test
    public void test2(){
        // 元素不重复
        // java.lang.IllegalArgumentException: duplicate element: 1
//        var set = Set.of("1", "a", "b", "1");

        var set = Set.of("Java", "Python");
        System.out.println(set);
    }

    @Test
    public void test3(){
        // key 不能重复
        // java.lang.IllegalArgumentException: duplicate key: 1
//        var map = Map.of(1, "Java", 1,"Python");

        var map = Map.of(1, "Java", 2,"Python");
        System.out.println(map);
    }
}
