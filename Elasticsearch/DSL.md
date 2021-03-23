## Domain Specific Language
### Query Context and Filter Context

查询语句的行为取决于它出在 query context 还是 filter context

处于 query context 的查询语句，回答 "这个文档对查询语句的匹配程度"，除了决定文档是否匹配，还会根据匹配程度得出一个分数。

处于 filter context 的查询语句，回答用户 "这个文档与查询语句匹配吗"，结果很简单，yes or no。不计算分数，仅用于过滤文档。

```python 
GET /_search
{
  "query": { 
    "bool": { 
      "must": [
        { "match": { "title":   "Search"        }},# query context
        { "match": { "content": "Elasticsearch" }}# query context
      ],
      "filter": [ 
        { "term":  { "status": "published" }},# filter context
        { "range": { "publish_date": { "gte": "2015-01-01" }}}# filter context
      ]
    }
  }
}
# 在 query context 中计算匹配度
# 在 filter context 中过滤文档
# 返回全部，并且所有文档评分为 1.0
# 这个基础评分可以修改
GET /_search
{
    "query": {
        "match_all": { "boost" : 1.2 }# 基础评分设定为1.2
    }
}
# 返回空
GET /_search
{
    "query": {
        "match_none": {}
    }
}
```

### 全文检索
```python 
# 全文检索用于对 full text field 进行全文查询。在进行查询前会对查询的字段进行分析。
## match
# 支持 text/number/date 
GET /_search
{
    "query": {
        "match" : {
            "message" : "this is a test"# 简单的匹配
        }
    }
}
# 它可以使用 逻辑运算
GET /_search
{
    "query": {
        "match" : {
            "message" : {
                "query" : "this is a test",
                "operator" : "and" # term 之间的匹配关系
                "should": 2# 最小匹配限定
            }
        }
    }
}
# 模糊匹配
GET /_search
{
    "query": {
        "match" : {
            "message" : {
                "query" : "this is a testt",
                "fuzziness": "AUTO"
            }
        }
    }
}

## match_phrase
# 短语匹配
GET /_search
{
    "query": {
        "match_phrase" : {
            "message" : "this is a test"
        }
    }
}

## match_phrase_prefix
# 前缀匹配
GET /_search
{
    "query": {
        "match_phrase_prefix" : {
            "message" : "quick brown f"
        }
    }
}

## multi_match
# 多字段匹配
GET /_search
{
  "query": {
    "multi_match" : {
      "query":    "this is a test", 
      "fields": [ "subject", "message" ] 
    }
  }
}

## query_tring
# 将查询字串按逻辑运算符拆分，并按逻辑运算符进行匹配
GET /_search
{
    "query": {
        "query_string" : {
            "default_field" : "content",
            "query" : "this AND that OR thus" # 使用逻辑运算符拆分
        }
    }
}
GET /_search
{
    "query": {
        "query_string" : {
            "default_field" : "content",
            "query" : "(new york city) OR (big apple)" 
        }
    }
}

## simple_query_string
# 类似 query_string ，对查询的内容按逻辑运算符分析，对于非法字串不报错，有特殊的字串分析语法
GET /_search
{
  "query": {
    "simple_query_string" : {
        "query": "\"fried eggs\" +(eggplant | potato) -frittata",
        "fields": ["title^5", "body"],
        "default_operator": "and"
    }
  }
}
# 除了 query_string 支持的逻辑运算符，还支持下列符号
+ and
| or
- not
" 将需要匹配的短语包含在内
* 通配符
() 优先匹配
~N  单词模糊匹配
~N  短语的模糊匹配
```

### Term-level query
术语查找，适合在文档中查找精确值，比如日期范围，email，ip等。
不同于全文搜索，术语查询不会进行分析，只会进行精确匹配。
```python
## term query 
# 在索引中查找包含完全一致的term的文档
# 可以跟不同的term不同的打分权重，基础分为1.0
GET _search
{
  "query": {
    "bool": {
      "should": [
        {
          "term": {
            "status": {
              "value": "urgent",
              "boost": 2.0 
            }
          }
        },
        {
          "term": {
            "status": "normal" 
          }
        }
      ]
    }
  }
}
# 使用 term query查找text类型的field，可能会匹配不到结果，因为text的字段经过 analyzer的分词，所以对于text类型应该用 match query，对于keyword类型应该用 term query

## Terms Query
# 多术语查找
# 查找包含任意属于的文档
GET /_search
{
    "query": {
        "terms" : { "user" : ["kimchy", "elasticsearch"]}
    }
}
# Terms lookup mechanismedit
# 术语查询构造器
# 当需要指定一个包含大量terms的terms过滤器时，可以从一个被索引的文档中获取这些terms的值。比如查找关注了你的人发的推特。目的时查推特，限定条件是关注了某人的人，所以可以从user的index中获取关注者列表，在从推特的index中查询，作者是这些人的推特。
# 过滤的维度，index type id path routing
PUT /users/_doc/2
{
    "followers" : ["1", "3"]# 关注者的id
}

PUT /tweets/_doc/1
{
    "user" : "1" # 作者 id 
}

GET /tweets/_search
{
    "query" : {
        "terms" : { # 注意这是一个 terms 查询
            "user" : { # 从另一个index中获取 terms 的值
                "index" : "users", # index 为user
                "type" : "_doc", # type 为_doc
                "id" : "2", # id 为 2
                "path" : "followers" # 查找的path
            }
        }
    }
}

## Terms Set Query
# 查询包含一个或多个指定 term 的文档，可以使用脚本或某个字段控制匹配的最小数目
# 使用字段控制
PUT /my-index/_doc/1?refresh
{
    "codes": ["ghi", "jkl"],
    "required_matches": 2 # mapping 中指定为 long 类型
}

PUT /my-index/_doc/2?refresh
{
    "codes": ["def", "ghi"],
    "required_matches": 2
}
GET /my-index/_search
{
    "query": {
        "terms_set": {
            "codes" : {
                "terms" : ["abc", "def", "ghi"],
                "minimum_should_match_field": "required_matches" # 指定最小匹配数
            }
        }
    }
}
# 使用脚本控制，更加的动态和灵活。
# params.num_terms 参数(filter 指定的 terms 个数)也可以作为指标
GET /my-index/_search
{
    "query": {
        "terms_set": {
            "codes" : {
                "terms" : ["abc", "def", "ghi"],
                "minimum_should_match_script": {
                   "source": "Math.min(params.num_terms, doc['required_matches'].value)"# 这里取两者较小的值，以防超过传入 terms 的最大数
                }
            }
        }
    }
}

## Range Query
# 范围查找
# 匹配具有特定范围字段值的文档，Lucene的查询取决于字段的类型，string使用TermRangeQuery，而date/number 使用NumbericRangeQuery
# 可使用的范围符：
gte     >= 
gt      >
lte     <=
lt      <
boost   加权值，基础值 1.0
# 对于 date range 的查询，可以使用 date math 进行日期运算
# date math 的 round 范围取决于使用的 range 符。精确至毫秒
# 例子：
# 大于 2020-11-18 的月份，不包含11月
gt 2020-11-18||/M  =>  2020-11-30T23:59:59.999(计算后的日期值)
# 大于等于 2020-11-18 的月份，包含11月
gte 2020-11-18||/M =>  2020-11-01
# 小于 2020-11-18 的月份，不包含11月
lt 2020-11-18||/M  =>  2020-11-01
# 小于等于 2020-11-18 的月份，包含11月
lte 2020-11-18||/M  =>  2020-11-30T23:59:59.999
# 格式化的日期需要指定它的format
GET _search
{
    "query": {
        "range" : {
            "born" : {
                "gte": "01/01/2012",
                "lte": "2013",
                "format": "dd/MM/yyyy||yyyy"# 日期格式
            }
        }
    }
}
# 日期格式如果跳过了某些部分，比如只指定了 dd ，则其余部分用UTC的起始时间填充，也就是1970-01-01T:00:00.000Z 来补足缺失的部分。
# 不同timezone的日期需要指定timezone
# timezone 指定的是条件的timezone，不是document数据的timezone，所以比较之前会将条件的日期真实值算出来
GET _search
{
    "query": {
        "range" : {
            "timestamp" : {
                "gte": "2015-01-01T00:00:00", # 实际是 2014-12-31T23:00:00 UTC
                "lte": "now",#直接使用 now时，它不受timezone影响，始终是UTC的当前时间，但如果使用 now/d 进行计算，则受timezone的影响 
                "time_zone": "+01:00" # 比较的时候会先进行timezone运算
            }
        }
    }
}
# 查找 range 类型的字段
# 将query range 与 field range 使用关系参数进行比较：
WITHIN      field range 在 query range 之内
CONTAINS    query range 在 field range 之内
INTERSECTS  field range 与 query range 相交

## Exists Query
# 查询存在字段且字段有值的文档
GET /_search
{
    "query": {
        "exists": {
            "field": "user" # 有user字段，且不为空
        }
    }
}
# field 字段必须指定，被指定的指端可以包含空串，比如 "" "_"，数组里面可以包含空元素，比如 [null,"foo"]，或者可以包含用户自定义的空值
# 想要实现反效果，也就是获取某个字段的值为空的文档，可以使用 bool 查询
GET /_search
{
    "query": {
        "bool": {
            "must_not": { # 这里是对限定条件取反
                "exists": {
                    "field": "user"
                }
            }
        }
    }
}

## Perfix Query
# 前缀查询，没啥好说的
GET /_search
{ "query": {
    "prefix" : { "user" : "ki" }
  }
}
GET /_search
{ "query": { # 同样，可以加权打分
    "prefix" : { "user" :  { "value" : "ki", "boost" : 2.0 } }
  }
}

## Wildcard Query
# 通配符查询
GET /_search
{
    "query": {
        "wildcard": {
            "user": { # 必须指定匹配的字段
                "value": "ki*y",
                "boost": 1.0,
                "rewrite": "constant_score"
            }
        }
    }
}
# 可用的通配符：
*   匹配 0 ~ n 个字符
?   匹配单个字符
# 注意不要用通配符开头，这会增加大量的查询开销
# rewrite 过程发生在 multi term 查询上，比如通配符查询，前缀查询。这些查询最终会经过 rewrite 处理。在 query_string 查询时也会发生。rewrite 的处理方式可以由下列参数控制：
constant_score 当需要匹配的term很少时，类似 constant_score_boolean，否则它顺序访问每个term，并对文档做标记，匹配上的文档被标记一个常量分数，也就是query中指定的 boost

scoring_boolean 首先将每个term转化为bool查询中的should子句，并且保持各自query的查询分数，这个分数对用户来说没用，但是它需要一个很强大的CPU去计算，所以constant_score总是很好用，因为分数超过1024，会引起should子句报错

constant_score_boolean 类似scoring_boolean，但是它不计算分数，它使用常量分数，也就是 boost 值，这样会避免 should 子句失败，如果 boost 没有超过 1024

top_terms_N 还是先转化为bool 查询的 should 子句，但是应用最高分数，N 控制取几个最高分。

top_terms_boost_N 类似top_terms_N，但是最高分作为 boost 进行计算。

top_terms_blended_freqs_N 按照匹配的出现频率打分

## Regexp Query
# 正则匹配
GET /_search
{
    "query": {
        "regexp":{
            "name.first": "s.*y"
        }
    }
}

## Fuzzy Query
# 模糊匹配，使用的是需要改动几个字符才能完全匹配的算法
GET /_search
{
    "query": {
        "fuzzy" : {
            "user" : {
                "value": "ki",
                "boost": 1.0,
                "fuzziness": 2,
                "prefix_length": 0,
                "max_expansions": 100
            }
        }
    }
}

## Type Query
# 类型匹配，没啥说的，按 type 查
GET /_search
{
    "query": {
        "type" : {
            "value" : "_doc"
        }
    }
}

## Ids Query
# 按 id 查
GET /_search
{
    "query": {
        "ids" : {
            "type" : "_doc",
            "values" : ["1", "4", "100"]
        }
    }
}
```

## Compound queries 复合查询
复合查询可以包装其他查询，组合各个查询的结果分数，更改他们的行为，切换他们的query context到filter context(他们对查询的行为表现不同)
```python 
## constant score query
# 包装一个 filter query，对每个结果返回一个固定等于 boost 的分数
GET /_search
{
    "query": {
        "constant_score" : {
            "filter" : { # filter context
                "term" : { "user" : "kimchy"}
            },
            "boost" : 1.2 # 可以指定 boost 的值，默认 1.0
        }
    }
}

## bool query
# 将其他查询boolean结合起来，对应 Lucene 的 BooleanQuery。它创建一个或多个boolean子句，每个子句都有一个类型：
must    匹配到的文档必须满足全部条件，且有助于得分
filter  匹配到的文档必须满足全部条件，但各个查询的score将被忽略，且运行在filter context下
should  匹配的文档应当满足条件，如果 bool query在query context中，且有 must 或者 filter 查询，那么匹配的文档可以不满足should中的任何条件，此时should的条件只会影响score。如果 bool 查询在 filter context中，并且没有must或者filter查询，那么匹配的文档至少要满足should 的一个条件，最少满足几个条件可以通过参数 minimum_should_match 来指定
must_not    匹配的文档不应满足任何一个条件
POST _search
{
  "query": {
    "bool" : {
      "must" : {
        "term" : { "user" : "kimchy" }
      },
      "filter": {
        "term" : { "tag" : "tech" }
      },
      "must_not" : {
        "range" : {
          "age" : { "gte" : 10, "lte" : 20 }
        }
      },
      "should" : [
        { "term" : { "tag" : "wow" } },
        { "term" : { "tag" : "elasticsearch" } }
      ],
      "minimum_should_match" : 1,
      "boost" : 1.0
    }
  }
}

## Dis Max query
# 最佳匹配查询，当要查询的内容存在于文档的多个字段时，根据最匹配的字段的分数作为返回。
GET /_search
{
    "query": {
        "dis_max" : {
            "tie_breaker" : 0.7,# 同一个 term 出现在不同的字段，比出现在同一个字段影响更大
            "boost" : 1.2,
            "queries" : [
                {
                    "term" : { "age" : 34 }
                },
                {
                    "term" : { "age" : 35 }
                }
            ]
        }
    }
}

## Function Score Query
# function_score 允许通过查询来修改检索文档的分数，适用于分数计算开销很大时候，可以将分数计算用于过滤之后的文档集。一个 function_score 必须要有一个 query 且有一个或多个 functions，用来为每个document重新计算分数
GET /_search
{
    "query": {
        "function_score": {
          "query": { "match_all": {} },# 不写等同于match_all
          "boost": "5", 
          "functions": [
              {
                  "filter": { "match": { "test": "bar" } },
                  "random_score": {}, # 打分模式
                  "weight": 23 # 权重
              },
              {
                  "filter": { "match": { "test": "cat" } },
                  "weight": 42
              }
          ],
          "max_boost": 42,
          "score_mode": "max",# 计算函数，有几个可选的计算方法
          "boost_mode": "multiply",
          "min_score" : 42 
        }
    }
}
# 首先，每个document根据function确定的算法算出score，然后 score_mode 指定了这些分数如何进行汇总。
# score_mode 分数计算方式(如何将多个functions的结果合成一个)
multiply sum avg first max min
# 新计算出来的分数于原有的query score进行合并
# boost_mode 分数合并方式(functions的最终结果与原query的分数的结合方式)
multiply replace sum avg max min
# 最终结果可以用评分进行筛选
min_score 
# 集中 function_score
script_score 
GET /_search
{
    "query": {
        "function_score": {
            "query": {
                "match": { "message": "elasticsearch" }
            },
            "script_score" : {
                "script" : {
                  "source": "Math.log(2 + doc['likes'].value)"
                }
            }
        }
    }
}
weight 权重值，用来与得分相乘
random_score 随机产生 0-1 之间，均匀分布的分数，不可重复，如果想要重复，可以适用 seed 和field字段来影响随机数的生成
GET /_search
{
    "query": {
        "function_score": {
            "random_score": {
                "seed": 10,
                "field": "_seq_no"
            }
        }
    }
}
field_value_factor 使用文档中的字段来影响分数，类似 script，多个字段只应用第一个
GET /_search
{
    "query": {
        "function_score": {
            "field_value_factor": {# sqrt(1.2 * doc['likes'].value)
                "field": "likes",
                "factor": 1.2,
                "modifier": "sqrt",
                "missing": 1
            }
        }
    }
}
decay  衰减函数，使用用户给定的原点到文档的数值类型字段(date,number,get_point等)，计算分数，必须指定 origin , scale

"DECAY_FUNCTION": { # linear, exp, gauss 三选一
    "FIELD_NAME": { # 函数名，随便起
          "origin": "11, 12",
          "scale": "2km", # 步长
          "offset": "0km", # 计算的初始值
          "decay": 0.33 # 每次衰减多少，默认 0.5
    }
}
GET /_search
{
    "query": {
        "function_score": {
            "gauss": {
                "date": {
                      "origin": "2013-09-17", # 不同类型的field需要给定不同类型的原点
                      "scale": "10d",
                      "offset": "5d", 
                      "decay" : 0.5 
                }
            }
        }
    }
}
```

### GeoShape Query
使用相同的网格坐标系对给定的 GEO shape和文档中的 GEO shape进行比较。
这个形状可以临时指定，也可以使用预先保存的形状
```python 
## Inline Shape Definition
GET /example/_search
{ # 查询的时候临时指定一个 shape
    "query":{
        "bool": {
            "must": {
                "match_all": {}
            },
            "filter": {
                "geo_shape": {
                    "location": {
                        "shape": {
                            "type": "envelope",
                            "coordinates" : [[13.0, 53.0], [14.0, 52.0]]
                        },
                        "relation": "within"
                    }
                }
            }
        }
    }
}

## Pre-Indexed Shape
# 使用预先创建的 shape
GET /example/_search
{
    "query": {
        "bool": {
            "filter": {
                "geo_shape": {
                    "location": {
                        "indexed_shape": { # 从哪里获取形状
                            "index": "shapes",
                            "type": "_doc",
                            "id": "deu",
                            "path": "location"
                        }
                    }
                }
            }
        }
    }
}

## Geo Bounding Box Query
# 基于一个矩形盒子边界对文档进行过滤
GET /_search
{
    "query": {
        "bool" : {
            "must" : {
                "match_all" : {}
            },
            "filter" : {
                "geo_bounding_box" : {
                    "pin.location" : { # 想要过滤的字段
                        "top_left" : { # 左上角
                            "lat" : 40.73,
                            "lon" : -74.1
                        },
                        "bottom_right" : { # 右下角
                            "lat" : 40.01,
                            "lon" : -71.12
                        }
                    }
                }
            }
        }
    }
}

## Geo Distance Query
# 基于到某个坐标点一定距离对文档进行过滤
# 例：查询某座标周围5km内的其他坐标
GET /my_locations/_search
{
    "query": {
        "bool" : {
            "must" : {
                "match_all" : {}
            },
            "filter" : {
                "geo_distance" : {
                    "distance" : "200km", # 注意距离单位
                    "pin.location" : { # 过滤的字段
                        "lat" : 40,
                        "lon" : -70
                    }
                }
            }
        }
    }
}

## Geo Polygon Query
# 基于坐标围成的多边形进行过滤，返回落在多边形内的文档
GET /_search
{
    "query": {
        "bool" : {
            "must" : {
                "match_all" : {}
            },
            "filter" : {
                "geo_polygon" : {
                    "person.location" : {
                        "points" : [
                            {"lat" : 40, "lon" : -70},
                            {"lat" : 30, "lon" : -80},
                            {"lat" : 20, "lon" : -90}
                        ]
                    }
                }
            }
        }
    }
}
```