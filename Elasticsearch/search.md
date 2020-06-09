## 查询

```python 
# 通常 search 操作会广播到所有的 index 和 shard 上，可以通过指定 routing 参数来指定查询哪个 shard 。
# 使用用户名进行 routing， 只查询 kimchy 发的 tweets
POST /twitter/_doc?routing=kimchy
{
    "user" : "kimchy",
    "post_date" : "2009-11-15T14:12:12",
    "message" : "trying out Elasticsearch"
}

# 对于 kimchy 发的 tweets 再进行过滤
POST /twitter/_search?routing=kimchy
{
    "query": {
        "bool" : {
            "must" : {
                "query_string" : {
                    "query" : "some query string here"
                }
            },
            "filter" : {
                "term" : { "user" : "kimchy" }
            }
        }
    }
}

# routing 可以是多个值， 由 ',' 分隔

# 开启自适应副本搜索
# coordinating node 将会寻找最佳的副本进行查询
PUT /_cluster/settings
{
    "transient": {
        "cluster.routing.use_adaptive_replica_selection": true
    }
}

# 可以使用 stats groups 将不同的聚合结果关联起来，方便后续使用 stats API进行查询
# 两个不同的组进行关联
POST /_search
{
    "query" : {
        "match_all" : {}
    },
    "stats" : ["group1", "group2"]
}

# 默认情况，ES 不限制搜索时命中的 shard 数。但是大量查询小 shard 会造成 CPU 和内存 的开销过大， 需要重新组织数据，变成少量的大 shard 。也可以手动设置同时可查的 shard 数
# cluster setting
action.search.shard_count.limit
# request parameter 
max_concurrent_shard_requests 

# 最大并发 256
```

### Search
The search API allows you to execute a search query and get back search hits that match the query. The query can either be provided using a simple query string as a parameter, or using a request body.

查询 API 允许你执行 search 并获取结果集。你可以使用 query string 或者 request body

#### URL srearch
```python 
# 用于快速的测试搜索
# 并非所有查询参数都公开 

GET /{index}/_search?{query string}

q   # 参照 query_string 的格式
df  # default field 没有指定查询 perfix 时默认查询的字段
analyzer    # 查询使用的分析器
analyze_wildcard    # 是否分析通配符和前缀 默认 false
batched_reduce_size # 多少个 shard 的结果集聚合成一个，减少开销
default_operator    # 默认逻辑操作符 OR AND， 默认 OR
lenient # 是否忽略类型转换失败， 默认 false
explain # 对于每个 hits 解释如何命中
stored_fields   # 指定返回的文档字段，不指定就不返回任何字段
sort    # 排序 fieldName:asc/fieldName:desc 或者 _score 基于分数排序
from    # 分页的起始偏移量，从 0 开始
size    # 分页大小
search_type # 查询方式 dfs_query_then_fetch / query_then_fetch
allow_partial_search_results    # 在部分结果失败时是否能看到部分成功的结果
```

#### Request Body Search
```python 
# index 支持通配符
# request body 使用 Query DSL
# GET 和 POST 方法都支持
GET /{index}/_search
{
    "query" : {
        "term" : { "user" : "kimchy" }
    }
}

# 快速查询是否有符合条件的文档(不关心文档内容)
GET /_search?q=message:number&size=0&terminate_after=1
size=0 # response 的 hits.hits 不包含 document 内容
terminate_after=1 # 搜索到 1 条符合条件的即返回结果

# 复杂查询，详情参考 DSL
GET /_search
{
    "query" : {
        "term" : { "user" : "kimchy" }
    }
}
# 分页查询， 在 body 体中增加 from 和 size
GET /_search
{
    "from" : 0, 
    "size" : 10,
    "query" : {
        "term" : { "user" : "kimchy" }
    }
}
# 排序，每个字段单独指定排序方式，或者使用 _score 进行排序
GET /my_index/_search
{
    "sort" : [
        { "post_date" : {"order" : "asc"}},
        "user",
        { "name" : "desc" },
        { "age" : "desc" },
        "_score"
    ],
    "query" : {
        "term" : { "user" : "kimchy" }
    }
}
# ES 支持使用一个数组或者包含多个值的字段进行排序，排序时需要指定这个字段的sort mode
min max 
sum avg median # 仅 array 类型字段
POST /_search
{
   "query" : {
      "term" : { "product" : "chocolate" }
   },
   "sort" : [
      {"price" : {"order" : "asc", "mode" : "avg"}}
   ]
}
# 嵌套结构的字段排序
POST /_search
{
   "query": {
      "nested": {
         "path": "parent",
         "query": {
            "bool": {
                "must": {"range": {"parent.age": {"gte": 21}}},
                "filter": {
                    "nested": {
                        "path": "parent.child",
                        "query": {"match": {"parent.child.name": "matt"}}
                    }
                }
            }
         }
      }
   },
   "sort" : [
      {
         "parent.child.age" : {
            "mode" :  "min",
            "order" : "asc",
            "nested": {
               "path": "parent",
               "filter": {
                  "range": {"parent.age": {"gte": 21}}
               },
               "nested": {
                  "path": "parent.child",
                  "filter": {
                     "match": {"parent.child.name": "matt"}
                  }
               }
            }
         }
      }
   ]
}
# 空值处理，当排序字段没有值时如何处理
_last # 置于最后
_first  #置于最前
customer value  # 自定义值
GET /_search
{
    "sort" : [
        { "price" : {"missing" : "_last"} }
    ],
    "query" : {
        "term" : { "product" : "chocolate" }
    }
}
# 未找到对应字段的处理，当本次 query 的文档都不包含这个排序字段时，如何进行处理
GET /_search
{
    "sort" : [
        { "price" : {"unmapped_type" : "long"} } # 当作 long 值处理
    ],
    "query" : {
        "term" : { "product" : "chocolate" }
    }
}

# 对于结果集的过滤， source filter
# 默认情况返回全部 _source 字段，除非使用了 stored_fields 或者  _source: false
# 通常不建议使用 stored_fields 来筛选字段
# _source 支持 wildcard patterns 来返回想要的结果
GET /_search
{
    "_source": {
        "includes": [ "obj1.*", "obj2.*" ],
        "excludes": [ "*.description" ]
    },
    "query" : {
        "term" : { "user" : "kimchy" }
    }
}

# 脚本字段 Script Fields
# 根据用户自定义的脚本进行计算，并返回的字段，对于每个 hit 的文档都应用
GET /_search
{
    "query" : {
        "match_all": {}
    },
    "script_fields" : {
        "test1" : { # 自定义字段
            "script" : {
                "lang": "painless",
                "source": "doc['price'].value * 2" # doc['price'] 取出文档中的 price句柄，doc['price'].value 取出这个句柄的值
            }
        },
        "test2" : {
            "script" : {
                "lang": "painless",
                "source": "doc['price'].value * params.factor",
                "params" : {
                    "factor"  : 2.0
                }
            }
        }
    }
}
# 直接访问原始 document
GET /_search
{
    "query" : {
        "match_all": {}
    },
    "script_fields" : {
        "test1" : {
            "script" : "params['_source']['message']" # params['_source']直接取到原始文档的值
        }
    }
}
# doc['filed'] 和 params['_source']['field'] 的区别在于
# doc[...] 每次都会将字段加载到内存，所以处理的很快，但会消耗更多内存，且只能处理简单字段
# params['_source'][...] 可以处理复杂字段，但是每次都要解析 document 所以效率很低

# 后筛选器 PostFilter
# 场景：对用户展示 Gucci 品牌/红色的产品，同时展示其他颜色的产品数。
# 既要展示所有 红色商品的详情，又要展示其他类别的总量
GET /shirts/_search
{
  "query": {
    "bool": {
      "filter": {
        "term": { "brand": "gucci" } # 主筛选器，选择全部 Gucci 牌子的产品
      }
    }
  },
  "aggs": {
    "colors": {
      "terms": { "field": "color" } # 主聚合，按照颜色分类
    },
    "color_red": {
      "filter": {
        "term": { "color": "red" } # 子聚合过滤器
      },
      "aggs": {
        "models": {
          "terms": { "field": "model" } # 子聚合，按照 model 分类
        }
      }
    }
  },
  "post_filter": { 
    "term": { "color": "red" } # 后筛选器，过滤掉其他颜色的内容，只保留红色
  }
}

# highlight 高亮检索结果
# 对检索结果的一个或多个字段获取高亮显示的片段，方便用户查看匹配到的位置。
# 三种 type 可选 unified plain fvh
unified # lucene 默认的高亮器，
plain # 标准的 lucene 高亮，适合在单个字段中突出显示简单的查询匹配
fvh # lucene 的 Fast Vector highlighter，可以用在设置了with_positions_offsets 为 term_vector 的字段上

# SearchType
# QUERY_THEN_FETCH 从各个shard上获取score等相关信息，但不返回 document 内容，在客户端进行重新排序后从各个shard取出需要的 document 返回给用户，默认的type。
# DFS_QUERY_THEN_FETCH 类似 QUERY_THEN_FETCH 但是发起查询之前会收集各个shard的文档频率和词频率，以便更准确的评分，结果集的size同用户设置的相同。

# Scroll 滚动文档
# 从单个搜索中返回大量结果，类似数据库的 curse ，从 scroll 请求中获取的结果始终保持请求时的状态，不会因索引的变更而有影响
# 要使用 scroll 请求，需要在请求路径里指定scroll保持的时间
POST /twitter/_search?scroll=1m
{
    "size": 100,
    "query": {
        "match" : {
            "title" : "elasticsearch"
        }
    }
}
# 返回的结果集中包含字段 _scroll_id， 需要在下次调用 scroll 时作为参数传入
POST /_search/scroll # 再次请求时不用指定 index
{
    "scroll" : "1m", # 刷新 scroll context 的时间
    "scroll_id" : "DXF1ZXJ5QW5kRmV0Y2gBAAAAAAAAAD4WYm9laVYtZndUQlNsdDcwakFMNjU1QQ==" # 每次的结果集中的 _scroll_id 字段
}
# 当 scroll 不再有结果时 hits.hits 为空数组
# 在调用 scroll 时也可以指定 size 作为每次返回的结果集的条目数
# 如果在 init scroll 请求中指定了聚合查询，只有第一次的返回结果有聚合的结果集
# 如果不考虑文档的顺序，需要指定使用 _doc 进行排序，以便获取更好的性能
GET /_search?scroll=1m
{
  "sort": [
    "_doc"
  ]
}
# 清除 scroll context
DELETE /_search/scroll
{
    "scroll_id" : "DXF1ZXJ5QW5kRmV0Y2gBAAAAAAAAAD4WYm9laVYtZndUQlNsdDcwakFMNjU1QQ=="
}
# 如果 scroll 返回的数量太多，可以进一步 slice
# 也就是对每次 scroll 本应返回的结果再次切分为若干切片
# 需要注意，如果 slice 的数量过多，首次查询会十分缓慢，默认最大支持 1024 个slice
GET /twitter/_search?scroll=1m
{
    "slice": {
        "id": 0, 
        "max": 2 
    },
    "query": {
        "match" : {
            "title" : "elasticsearch"
        }
    }
}
# 可以选择一个 doc_value 的字段进行 slice
GET /twitter/_search?scroll=1m
{
    "slice": {
        "field": "date",
        "id": 0,
        "max": 10
    },
    "query": {
        "match" : {
            "title" : "elasticsearch"
        }
    }
}

# preference 查询偏好，设定查询哪个 shard ，通常不使用
# Sequence Numbers and Primary Term 返回序列号和主序，用于并发控制(乐观锁)
# Version 返回版本号，用于并发控制(乐观锁)
# Inner hits 父子文档关联查询

# Field Collapsing 字段折叠
# 将结果集按照某个字段进行折叠(这个字段可以视为分组标准，或者父/子关系中的父)
# 折叠后的结果集，默认对于每个 collapse 字段仅返回 top hit
# collapse 字段只能是 numbers 或 keywords 类型
GET /twitter/_search
{
    "query": {
        "match": {
            "message": "elasticsearch"
        }
    },
    "collapse" : {
        "field" : "user" # 按用户折叠
    },
    "sort": ["likes"], # 按喜好数进行排序
    "from": 10 
}
# 展开 collapse 
GET /twitter/_search
{
    "query": {
        "match": {
            "message": "elasticsearch"
        }
    },
    "collapse" : {
        "field" : "user", 
        "inner_hits": [
            {
                "name": "most_liked",  
                "size": 3,
                "sort": ["likes"]
            },
            {
                "name": "most_recent", 
                "size": 3,
                "sort": [{ "date": "asc" }]
            }
        ]
    },
    "sort": ["likes"]
}

# 深度分页
# from + size 仅支持10000 以内，且效率很低
# scroll 支持大量查询分页，但是不能实时反应用户对文档的操作
# Search After 活动分页查询，使用上一次查询的结果来确定下一次的查询
# 原理是利用排序的字段值，再下次查询时传入 search_after， 以便从上次查询的位置之后返回结果集 
# 首次
GET twitter/_search
{
    "size": 10, # 分页大小
    "query": {
        "match" : {
            "title" : "elasticsearch"
        }
    },
    "sort": [
        {"date": "asc"},
        {"tie_breaker_id": "asc"} # 自定义字段，对 _id 字段的doc_value型copy，用来作为分页的标识   
    ]
}
# 二次
GET twitter/_search
{
    "size": 10,
    "query": {
        "match" : {
            "title" : "elasticsearch"
        }
    },
    "search_after": [1463538857, "654323"],
    "sort": [
        {"date": "asc"},
        {"tie_breaker_id": "asc"}
    ]
}
# 由于文档是动态的，所以可能由于用户的修改造成排序不准确，而且无法跳页访问。

# Search Template 查询模板
# 由于经常大量使用的查询，为了减少代码量，避免每次都进行修改，可以创建查询模板，并再真正查询时至传入必要参数
POST _scripts/<templateid>
{
    "script": {
        "lang": "mustache", # mustache 语法
        "source": {
            "query": {
                "match": {
                    "title": "{{query_string}}"
                }
            }
        }
    }
}
GET _search/template
{
    "id": "<templateid>", 
    "params": {
        "query_string": "search for these words"
    }
}
```