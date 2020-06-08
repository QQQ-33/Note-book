### 对 JSON 数据类型的操作


```sql 
### json & jsonb 操作符
-- -> 获取 json 数组某个索引的元素，从 0 开始，返回 json
select '[{"a":"1"},{"a":"2"},{"a":"3"}]'::json -> 2
-- {"a":"3"}

-- -> 以 key 获取对象，返回 json
select '{"a":{"b":"c"}}'::json -> 'a'
-- {"b":"c"}

-- ->> 获取 json 数组某个索引的元素，从 0 开始，返回 text
select '[{"a":"1"},{"a":"2"},{"a":"3"}]'::json ->> 2
-- '{"a":"3"}'

-- ->> 以 key 获取对象，返回 text
select '{"a":{"b":"c"}}'::json ->> 'a'
-- '{"b":"c"}'

-- #> 获取指定路径上的对象，返回 json
select '{"a":{"b":{"c":"d"}}}'::json #> '{a,b}'
-- {"c":"d"}

-- #>> 获取指定路径上的对象，返回 text
select '{"a":{"b":{"c":"d"}}}'::json #> '{a,b}'
-- '{"c":"d"}'

-- 总结： > 返回 json 对象， >> 返回 json 文本
```

```sql 
### jsonb 操作符
-- 判断 jsonb 是否包含部分键值，仅判断顶级路径, 返回 boolean 值， 此符号可以 <@ 或者 @> 使用
select '{"a":{"c":"1"}, "b":"2"}'::jsonb @> '{"b":"2"}' -- true
select '{"a":{"c":"1"}, "b":"2"}'::jsonb @> '{"c":"1"}' -- false

-- 判断字符串是否存在于 jsonb 中，仅判断顶级路径，返回 boolean 值
select '{"a":{"c":"1"}, "b":"2"}'::jsonb ? 'b' -- true
select '{"a":{"c":"1"}, "b":"2"}'::jsonb ? 'c' -- false

-- 判断字符串数组中任意字串是否存在于 jsonb 中，仅判断顶级路径，返回 boolean 值
select '{"a":{"c":"1"}, "b":"2"}'::jsonb ?| array['b','c'] -- true

-- 将两个 jsonb 连接为新 jsonb 值，相同的 key 右边覆盖左边
select '{"a":{"c":"1"}, "b":"2"}'::jsonb || '{"d":{"e":"1"}, "f":"2"}'::jsonb 
-- '{"a": {"c": "1"}, "b": "2", "d": {"e": "1"}, "f": "2"}'
select '["a","b"]'::jsonb || '["c","d"]'::jsonb 
-- '["a", "b", "c", "d"]'

-- 从 jsonb 中删除 键/键值对 ，返回新 jsonb
select '{"a":{"c":"1"}, "b":"2"}'::jsonb - 'a'
-- {"b":"2"}
-- select '{"a": "b", "c": "d"}'::jsonb - '{a,c}'::text[]
-- 9.6 测试未通过，有待验证

-- 从 json 数组中删除指定索引的元素，顶层必须为数组
select '["a", "b"]'::jsonb - 1
-- ["a"]
-- 从 json 中删除指定路径的元素
select '["a", {"b":1}]'::jsonb #- '{1,b}'
-- ["a", {}]
```

```sql 
### json & jsonb 的创建
-- 将目标转化为 json， 如果没有映射，将按照文本进行转换 
select to_json('{"a":1, "b":true, "c":"d"}' :: text)
-- "{\"a\":1, \"b\":true, \"c\":\"d\"}"

-- 数组转 json ， 多维数组会成为 多维 json 数组。 第二个参数可以在第一维度元素直接增加换行符
select array_to_json('{{1,5},{99,100}}'::int[]) 
-- [[1,5],[99,100]]
select array_to_json('{{1,5},{99,100}}'::int[], true)
-- [[1,5],  [99,100]]

-- row 对象转数组，多用于将查询的结果集以 json 形式返回，很常用！
select row_to_json(row(1,'foo'))
-- {"f1":1,"f2":"foo"}

-- 从可变参数列表构建包含不同类型元素的 json 数组
select json_build_array(1,2,'3',true,5)
-- [1, 2, "3", true, 5]

-- 从可变参数列表构造 json 对象，参数列表是交替出现的 key， value
select json_build_object('foo',1,'bar',2)
-- {"foo" : 1, "bar" : 2}

-- 从文本构建 json ，参数是偶数个元素的数组(成员被交替作为 K 或 V)，或者二维数组
select json_object('{a, 1, b, "def", c, 3.5}') -- 偶数个元素
select json_object('{{a, 1}, {b, "def"}, {c, 3.5}}') -- 二维数组
-- {"a" : "1", "b" : "def", "c" : "3.5"}

-- 从两个数组构建一个 json， 一个为Key的数组，一个为value的数组
select json_object('{a, b}', '{1,2}') 
-- {"a" : "1", "b" : "2"}
```

```sql 
### json 的处理和分析
-- json 数组的长度
select 	json_array_length('[1,2,3,{"f1":1,"f2":[5,6]},4]')
-- 5

-- 按顶级展开 json 对象，逐行返回结果
select * from json_each('{"a":{"c": 1}, "b":"bar"}')	
/*
 key | value
-----+-------
 a   | {"c": 1}
 b   | "bar"
*/

-- 按顶级展开 json 对象，逐行返回 text 类型结果
select * from json_each_text('{"a":"foo", "b":"bar"}')
/*
 key | value
-----+-------
 a   | foo
 b   | bar
*/

-- 返回指定路径的 json 值， 等同于 #> 操作符
select json_extract_path('{"f2":{"f3":1}, "f4":{"f5":99, "f6":"foo"}}','f4')
-- {"f5":99,"f6":"foo"}

-- 返回指定路径的 text 值， 等同于 #>> 操作符
select json_extract_path_text('{"f2":{"f3":1},"f4":{"f5":99,"f6":"foo"}}','f4', 'f6')
-- 'foo'

-- 返回顶级 json 对象的 key 集合， 并展开返回结果
select 	json_object_keys('{"f1":"abc","f2":{"f3":"a", "f4":"b"}}')
/*
 json_object_keys
------------------
 f1
 f2
*/

-- 按指定的表匹配格式， 从 json 返回一个表的 record
-- null::myrowtype 要指定一个已经存在的表名，例如 null:"master_data.customer"
-- json 对象的格式也要与表中的字段相对应
select * from json_populate_record(null::myrowtype, '{"a": 1, "b": ["2", "a b"], "c": {"d": 4, "e": "a b c"}}')
/*
 a |   b       |      c
---+-----------+-------------
 1 | {2,"a b"} | (4,"a b c")

 myrowtype 为一张表， a, b, c 是表中的字段
*/

-- 按指定的表匹配格式， 从 json 数组返回一个表的多行 record
-- null::myrowtype 要指定一个已经存在的表名，例如 null:"master_data.customer"
-- json 对象的格式也要与表中的字段相对应
select * from json_populate_recordset(null::myrowtype, '[{"a":1,"b":2},{"a":3,"b":4}]')
/*
 a | b
---+---
 1 | 2
 3 | 4

 myrowtype 为一张表， a, b 是表中的字段
*/

-- 将 json 数组展开为多行 json 对象
select * from json_array_elements('[1, true, [2,false]]')
/*
   value
-----------
 1
 true
 [2,false]
*/

-- 将 json 数组展开为多行 text 对象
select * from json_array_elements_text('["foo", "bar"]')
/*
   value
-----------
 foo
 bar
*/

-- 返回顶级元素的类型
select json_typeof('-123.4') -- number
select json_typeof('"aaa"') -- string
select json_typeof('["foo", "bar"]') -- array
select json_typeof('{"a":1,"b":2}') -- object
select json_typeof('true') -- boolean
select json_typeof('null') -- null

-- 自定义一个 record 的结构，并从 json 返回它，必须用一个AS子句显式地定义记录的结构
select * from json_to_record('{"a":1,"b":[1,2,3],"c":[1,2,3],"e":"bar","r": {"a": 123, "b": "a b c"}}') as x(a int, b text, c int[], d text, r myrowtype)
-- 9.6 测试结果显示 "c":[1,2,3] 无法解析成 int[],

-- 将 json 数组按自定义结构解析为多行 record
select * from json_to_recordset('[{"a":1,"b":"foo"},{"a":"2","c":"bar"}]') as x(a int, b text);
/*
 a |  b
---+-----
 1 | foo
 2 |
*/

-- 返回带有 null 值的对象域
select json_strip_nulls('[{"f1":1,"f2":{"a":null}},null,{"a":null,"b":1},3]')-- 	[{"f1":1,"f2":{}}, null, {"b":1}, 3]
-- 测试结果： 返回非空的 对象域， null值视为非空。

-- 补充 jsonb 对象，jsonb_set(target jsonb, path text[], new_value jsonb[, create_missing boolean])
-- 如果create_missing 为 true (缺省是true)， 将创建 path 指定的部分，返回最终的 jsonb 对象 ， new_value 为 path 指定部分的值
select jsonb_set('[{"f1":1,"f2":null},2,null,3]', '{0,f1}','[2,3,4]', false)
-- [{"f1":[2,3,4],"f2":null},2,null,3]
select jsonb_set('[{"f1":1,"f2":null},2]', '{0,f3}','[2,3,4]')
-- [{"f1": 1, "f2": null, "f3": [2, 3, 4]}, 2]

-- 在 jsonb 中插入新值，只有 path指定的部分不存在才插入，insert_after决定前置还是后置插入，默认 false， 前置插入
-- jsonb_insert(target jsonb, path text[], new_value jsonb, [insert_after boolean])
select jsonb_insert('{"a": [0,1,2]}', '{a, 1}', '"new_value"')
-- {"a": [0, "new_value", 1, 2]}
select jsonb_insert('{"a": [0,1,2]}', '{a, 1}', '"new_value"', true)
-- {"a": [0, 1, "new_value", 2]}

-- 格式化 json 并返回 text
select jsonb_pretty('[{"f1":1,"f2":null},2,null,3]')	
/*
[
    {
        "f1": 1,
        "f2": null
    },
    2,
    null,
    3
]
*/

```