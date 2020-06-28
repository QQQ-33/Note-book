## Aggreations 聚合查询
聚合框架用于查询聚合的数据，聚合可以看作是在一组文档之上进行分析的工作单元，execution query 定义了它将会作用于哪些文档，聚合操作被分为4大类：

### Bucketing
> A family of aggregations that build buckets, where each bucket is associated with a key and a document criterion. When the aggregation is executed, all the buckets criteria are evaluated on every document in the context and when a criterion matches, the document is considered to "fall in" the relevant bucket. By the end of the aggregation process, we’ll end up with a list of buckets - each one with a set of documents that "belong" to it.

> 一组构建 bucket 的聚合操作。每个 bucket 用 key 以及文档条件进行关联。执行聚合时，对上下文中的每个文档计算 bucket 的条件，满足条件的文档被视为属于某bucket。聚合结束时，得到一个bucket的列表，每个bucket都有属于它的一组文档。

### Metric
> Aggregations that keep track and compute metrics over a set of documents.

> 在一组文档上进行聚合

### Matrix
> A family of aggregations that operate on multiple fields and produce a matrix result based on the values extracted from the requested document fields. Unlike metric and bucket aggregations, this aggregation family does not yet support scripting.

> 对多个字段进行聚合操作，生成一个结果矩阵，不支持 script 查询

### Pipeline
> Aggregations that aggregate the output of other aggregations and their associated metrics.

> 对其他聚合的结果进行聚合


```python 
# 聚合查询可以嵌套，且没有嵌套深度的限制
# 聚合查询的标准结构
"aggs" : { # aggs 或者 aggregations ,持有聚合的结果
    "<aggregation_name>" : { # 自定义的聚合名称，在response中会有体现
        "<aggregation_type>" : { # 聚合类型，就是上面4种
            <aggregation_body>
        }
        [,"meta" : {  [<meta_data_body>] } ]?
        [,"aggregations" : { [<sub_aggregation>]+ } ]?
    }
    [,"<aggregation_name_2>" : { ... } ]*
}
```

### Metrics Aggs

```python 
# 基于从文档中的字段提取的值进行聚合
# Numeric metric 聚合
# 有单个返回值的聚合，多个返回值的聚合
# 多返回值的聚合通常充当子聚合

## avg 
# 平均值，单返回值
POST /exams/_search?size=0
{
    "aggs" : {
        "avg_grade" : { "avg" : { "field" : "grade" } }
    }
}

## weighted_avg
# 加权平均，单返回值
# 公式 ∑(value * weight) / ∑(weight)
# 常规 avg 相当于所有权重为 1
POST /exams/_search
{
    "size": 0,
    "aggs" : {
        "weighted_grade": {
            "weighted_avg": {
                "value": {
                    "field": "grade" # 指定字段
                },
                "weight": {
                    "field": "weight" # 指定权重值，这里是从文档的字段获取
                }
            }
        }
    }
}
POST /exams/_search
{
    "size": 0,
    "aggs" : {
        "weighted_grade": {
            "weighted_avg": {
                "value": {
                    "script": "doc.grade.value + 1"
                },
                "weight": {
                    "script": "doc.weight.value + 1" # 这里通过脚本计算权重值
                }
            }
        }
    }
}

## cardinality
# 统计不同值的的数量
# 例： 统计有多少不同类型的商品
POST /sales/_search?size=0
{
    "aggs" : {
        "type_count" : {
            "cardinality" : {
                "field" : "type" # 统计有多少不同的type
            }
        }
    }
}
# precision_threshold 计算精度，当结果小于这个值，统计的是准确值，大于这个值，则统计的结果是模糊的近似值
POST /sales/_search?size=0
{
    "aggs" : {
        "type_count" : {
            "cardinality" : {
                "field" : "_doc",
                "precision_threshold": 100 # 默认3000
            }
        }
    }
}

## stats
# 多维度聚合，多个返回值
# 固定返回 min max sum count avg 5个维度
POST /exams/_search?size=0
{
    "aggs" : {
        "grades_stats" : { "stats" : { "field" : "grade" } }
    }
}

## extended_stats
# stats的扩展，多返回值
# 除了 stats 的5个返回值，还返回 sum_of_squares variance std_deviation std_deviation_bounds
GET /exams/_search
{
    "size": 0,
    "aggs" : {
        "grades_stats" : { "extended_stats" : { "field" : "grade" } }
    }
}

## geo_bounds
# 统计指定范围内有多少点
POST /museums/_search?size=0
{
    "query" : {
        "match" : { "name" : "musée" }
    },
    "aggs" : {
        "viewport" : {
            "geo_bounds" : {
                "field" : "location", 
                "wrap_longitude" : true 
            }
        }
    }
}

## geo_centroid
# 加权算质心的坐标
POST /museums/_search?size=0
{
    "aggs" : {
        "centroid" : {
            "geo_centroid" : {
                "field" : "location" 
            }
        }
    }
}

## max
# 最大值
POST /sales/_search?size=0
{
    "aggs" : {
        "max_price" : { "max" : { "field" : "price" } }
    }
}

## min
# 最小值
POST /sales/_search?size=0
{
    "aggs" : {
        "min_price" : { "min" : { "field" : "price" } }
    }
}

## percentiles
# 百分位数，第n个百分位数表示比 n% 的观测值都大的值
# 常用于正态分布
GET latency/_search
{
    "size": 0,
    "aggs" : {
        "load_time_outlier" : {
            "percentiles" : {
                "field" : "load_time" 
            }
        }
    }
}
{
    ...

   "aggregations": {
      "load_time_outlier": {
         "values" : {# 默认返回的百分比
            "1.0": 5.0, 
            "5.0": 25.0,
            "25.0": 165.0,
            "50.0": 445.0,
            "75.0": 725.0,
            "95.0": 945.0, # 945 比 95% 的数据都大
            "99.0": 985.0
         }
      }
   }
}
GET latency/_search
{
    "size": 0,
    "aggs" : {
        "load_time_outlier" : {
            "percentiles" : {
                "field" : "load_time",
                "percents" : [95, 99, 99.9] # 指定返回的百分比范围
            }
        }
    }
}

## percentile_ranks
# 百分比排名，计算字段值处于整体的百分比
GET latency/_search
{
    "size": 0,
    "aggs" : {
        "load_time_ranks" : {
            "percentile_ranks" : {
                "field" : "load_time", 
                "values" : [500, 600]
            }
        }
    }
}
{
    ...

   "aggregations": {
      "load_time_ranks": {
         "values" : {
            "500.0": 90.01,
            "600.0": 100.0
         }
      }
   }
}

## scripted_metric
# 聚合脚本，按照特定顺序执行的一组脚本
# init_script map_script combine_script reduce_script
# 
POST ledger/_search?size=0
{
    "query" : {
        "match_all" : {}
    },
    "aggs": {
        "profit": {
            "scripted_metric": {
                "init_script" : "state.transactions = []",
                "map_script" : "state.transactions.add(doc.type.value == 'sale' ? doc.amount.value : -1 * doc.amount.value)", 
                "combine_script" : "double profit = 0; for (t in state.transactions) { profit += t } return profit",
                "reduce_script" : "double profit = 0; for (a in states) { profit += a } return profit"
            }
        }
    }
}

## sum
# 
POST /sales/_search?size=0
{
    "query" : {
        "constant_score" : {
            "filter" : {
                "match" : { "type" : "hat" }
            }
        }
    },
    "aggs" : {
        "hat_prices" : { "sum" : { "field" : "price" } }
    }
}

## top_hits 
# 分组 排序 取前几
POST /sales/_search?size=0
{
    "aggs": {
        "top_tags": {
            "terms": {
                "field": "type",
                "size": 3
            },
            "aggs": {
                "top_sales_hits": {
                    "top_hits": {
                        "sort": [
                            {
                                "date": {
                                    "order": "desc"
                                }
                            }
                        ],
                        "_source": {
                            "includes": [ "date", "price" ]
                        },
                        "size" : 1
                    }
                }
            }
        }
    }
}

## value_count
# 计数统计
POST /sales/_search?size=0
{
    "aggs" : {
        "types_count" : { "value_count" : { "field" : "type" } }
    }
}

## median_absolute_deviation
# 绝对中位数偏差
```

### Bucket Agg
Bucket不进行计算，而是对document进行分组。可以在bucket的基础上继续进行 metrics 聚合

```python 
## Histogram
# 直方图，利用从文档中获取的值，动态的构建固定间隔的 bucket。
# 下取整
bucket_key = Math.floor((value - offset) / interval) * interval + offset
POST /sales/_search?size=0
{
    "aggs" : {
        "prices" : {
            "histogram" : {
                "field" : "price",
                "interval" : 50
            }
        }
    }
}
{
    ...
    "aggregations": {
        "prices" : {
            "buckets": [
                {
                    "key": 0.0,
                    "doc_count": 1
                },
                {
                    "key": 50.0,
                    "doc_count": 1
                },
                {
                    "key": 100.0,
                    "doc_count": 0
                },
                {
                    "key": 150.0,
                    "doc_count": 2
                },
                {
                    "key": 200.0,
                    "doc_count": 3
                }
            ]
        }
    }
}
# 默认情况，返回所有 bucket ，可以通过设置 min_doc_count 来规定，bucket内包含至少多少个文档时才会返回。
POST /sales/_search?size=0
{
    "aggs" : {
        "prices" : {
            "histogram" : {
                "field" : "price",
                "interval" : 50,
                "min_doc_count" : 1# 至少有1个文档才会返回
            }
        }
    }
}
# 默认情况下，bucket 取值的上下限时根据文档中的值确定的，但是如果需要超过文档中的最大值或者最小值，可以手动指定 bucket的范围
POST /sales/_search?size=0
{
    "query" : {
        "constant_score" : { "filter": { "range" : { "price" : { "to" : "500" } } } }
    },
    "aggs" : {
        "prices" : {
            "histogram" : {
                "field" : "price",
                "interval" : 50,
                "extended_bounds" : { # 扩展 bucket 的范围
                    "min" : 0,
                    "max" : 500
                }
            }
        }
    }
}

## Date Histogram
# 日期直方图，类似直方图，只不过支持日期格式，日期计算等
# interval 可以指定日期单位
ms s m h d w M q y
POST /sales/_search?size=0
{
    "aggs" : {
        "sales_over_time" : {
            "date_histogram" : {
                "field" : "date",
                "interval" : "1M",
                "format" : "yyyy-MM-dd", 
                "time_zone" : "UTC" 
            }
        }
    }
}
{
    ...
    "aggregations": {
        "sales_over_time": {
            "buckets": [
                {
                    "key_as_string": "2015-01-01",
                    "key": 1420070400000,
                    "doc_count": 3
                },
                {
                    "key_as_string": "2015-02-01",
                    "key": 1422748800000,
                    "doc_count": 2
                },
                {
                    "key_as_string": "2015-03-01",
                    "key": 1425168000000,
                    "doc_count": 2
                }
            ]
        }
    }
}

## Auto-interval Date Histogram
# 类似 Date Histogram 但是无需指定 interval， 需要指定bucket的数量，然后自动将数据分入这些bucket中
POST /sales/_search?size=0
{
    "aggs" : {
        "sales_over_time" : {
            "auto_date_histogram" : {
                "field" : "date",
                "buckets" : 10
            }
        }
    }
}

## Range 
# 按照范围分组 from to
GET /_search
{
    "aggs" : {
        "price_ranges" : {
            "range" : {
                "field" : "price",
                "keyed" : true,
                "ranges" : [
                    { "key" : "cheap", "to" : 100 },
                    { "key" : "average", "from" : 100, "to" : 200 },
                    { "key" : "expensive", "from" : 200 }
                ]
            }
        }
    }
}
{
    ...
    "aggregations": {
        "price_ranges" : {
            "buckets": {
                "cheap": {
                    "to": 100.0,
                    "doc_count": 2
                },
                "average": {
                    "from": 100.0,
                    "to": 200.0,
                    "doc_count": 2
                },
                "expensive": {
                    "from": 200.0,
                    "doc_count": 3
                }
            }
        }
    }
}

## Date Range
# 类似 Range 支持日期的操作
POST /sales/_search?size=0
{
    "aggs": {
        "range": {
            "date_range": {
                "field": "date",
                "format": "MM-yyy",
                "ranges": [
                    { "from": "01-2015",  "to": "03-2015", "key": "quarter_01" },
                    { "from": "03-2015", "to": "06-2015", "key": "quarter_02" }
                ],
                "keyed": true
            }
        }
    }
}
```