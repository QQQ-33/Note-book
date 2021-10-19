### ES
> Elasticsearch 是一个分布式的搜索和分析引擎
> 提供近实时的搜索和分析，可以保存各种类型的数据，结构化，非结构化，数字，地理信息。
> ES 以文档形式保存复杂的数据结构，以 JSON 格式序列化。当 ES 以集群形式存在，document 存在于各个节点，可以从任意节点快速访问。
> ES利用***倒排索引***对文档中的每个单词进行索引。
> index 可以看作是 document 的优化集合，document 是 fields 的集合。默认情况，ES对所有 field 进行索引，并根据数据类型选择专用的数据结构。
> text使用 inverted indeices ， number 和 geo 使用 BKD trees（空间坐标查找优化）。

> ES可以根据 document 的 fields 动态处理，当有新的field加入时候自动加入到index中。
> 当然，也可以预先定义好 mapping ，手动管理每个 field如何 store 和 index。

> Elasticsearch 提供可简单的 REST API 来管理集群，搜索数据。
> 你可以进行结构化查询或者全文检索。
> 可以用 JSON 风格的查询语言（DSL）或者 SQL 风格的查询语言，也可以通过 JDBC & ODBC进行访问。
> 使用聚合函数实时分析数据。
> 搜索和聚合可以同时进行，聚合将在符合条件的数据中进行。

> Elasticsearch 的 index 只是逻辑上的一个或一组物理 shards ， 每个 shard 是一个独立的 index 。index 中的 document 分布在每个 shard 中。 
> shard的数量要视情况而定，shard的大小和数量会影响查询的效率和集群维护的开销
> （个人理解，shard是对index的分割，每个shard持有一部分的 index document，所有shard合在一起组成一个完整的index，而每个shard又是一个独立的index，所以查询的时候可以并行查询每个shard，之后合并结果。对于shard数量的设置需要根据实际情况考虑，最优情况是node = shard数，但是不宜过多，6.0之后已经支持shard自动扩展。还要保证shard的大小，处于合适的范围）

```javaScript
/**
* Near Realtime（NRT） 近实时。数据提交索引后，立马就可以搜索到。 
* Cluster 集群，一个集群由一个唯一的名字标识，默认为“elasticsearch”。集群名称非常重要，具有相同集群名的节点才会组成一个集群。集群名称可以在配置文件中指定。 
* Node 节点：存储集群的数据，参与集群的索引和搜索功能。像集群有名字，节点也有自己的名称，默认在启动时会以一个随机的UUID的前七个字符作为节点的名字，你可以为其指定任意的名字。通过集群名在网络中发现同伴组成集群。一 个节点也可是集群。 
* Index 索引: 一个索引是一个文档的集合（等同于solr中的集合）。每个索引有唯一的名字，通过这个名字来操作它。一个集群中可以有任意多个索引。 
* Type 类型：指在一个索引中，可以索引不同类型的文档，如用户数据、博客数据。从6.0.0 版本起已废弃，一个索引中只存放一类数据。 
* Document 文档：被索引的一条数据，索引的基本信息单元，以JSON格式来表示。 
* Shard 分片：在创建一个索引时可以指定分成多少个分片来存储。每个分片本身也是一个功能完善且独立的“索引”，可以被放置在集群的任意节点上。 Replication 备份: 一个分片可以有多个备份（副本）
*/
```
#### 索引模板
```javascript
// Index mapping
// 模板有两种类型：索引模板和组件模板。
// 组件模板是可重用的构建块，用于配置映射，设置和别名；它们不会直接应用于一组索引。
// 索引模板可以包含组件模板的集合，也可以直接指定设置，映射和别名。

// 
```

### Elasticsearch 基本操作
```python 
# 单个 document
PUT https://endpoint/{index}/{type}/{id}
{
  "name": "Tom Yang"
}

GET https://endpoint/{index}/{type}/{id}
{
	'_index': 'csn33188974',
	'_type': 'ngdi',
	'_id': '1858af77-c48c-3561-a835-dbe3da392bc7',
	'_version': 1,
	'found': True,
	'_source': {
        "name": "Tom Yang"
    }
}

# 批量上传 document
POST https://endpoint/_bulk
# 文档以 \n 进行分行，且文档结尾必须是 \n (json 文档不能使用美化后的 json)
# {"Content-Type": "application/json"}
{ "index" : { "_index" : "test", "_type" : "_doc", "_id" : "1" } }
{ "field1" : "value1" }
{ "delete" : { "_index" : "test", "_type" : "_doc", "_id" : "2" } }
{ "create" : { "_index" : "test", "_type" : "_doc", "_id" : "3" } }
{ "field1" : "value3" }
{ "update" : {"_id" : "1", "_type" : "_doc", "_index" : "test"} }
{ "doc" : {"field2" : "value2"} }
...

# 可以批量进行的操作有， index, create, delete, update
# 最佳的 bulk 大小需要根据 document 的大小及复杂度来定
```

### Search
```python 
# ES 使用 DSL 进行查询
GET /{index}/{type}/{id}/_search
# request
{
  "query": { "match_all": {} },
  "sort": [
    { "account_number": "asc" }
  ]
}
# response
{
  "took" : 63,
  "timed_out" : false,
  "_shards" : {
    "total" : 5,
    "successful" : 5,
    "skipped" : 0,
    "failed" : 0
  },
  "hits" : {
    "total" : 1000,
    "max_score": null,
    "hits" : [ {
      "_index" : "bank",
      "_type" : "_doc",
      "_id" : "0",
      "sort": [0],
      "_score" : null,
      "_source" : {"account_number":0,"balance":16623,"firstname":"Bradshaw","lastname":"Mckenzie","age":29,"gender":"F","address":"244 Columbus Place","employer":"Euron","email":"bradshawmckenzie@euron.com","city":"Hobucken","state":"CO"}
    }, ...
    ]
  }
}

```
- took – how long it took Elasticsearch to run the query, in milliseconds
- timed_out – whether or not the search request timed out
- _shards – how many shards were searched and a breakdown of how many shards succeeded, failed, or were skipped.
- max_score – the score of the most relevant document found
- hits.total.value - how many matching documents were found
- hits.sort - the document’s sort position (when not sorting by relevance score)
- hits._score - the document’s relevance score (not applicable when using match_all)

```python 
# 简单查询
GET /{index}/_search
{
	"size": 10,
	"query": {
		"match": {
			"oper_mode": "POWER"
		}
	}
}
# 多条件查询
{
	"size": 10,
	"query": {
		"bool": {
			"must": [{
				"match": {
					"oper_mode": "POWER"
				}
			}],
			"must_not": [{
				"match": {
					"horse_power": "0"
				}
			}]
		}
	}
}
```

```python 
# 简单聚合与分析
GET /{index}/_search
# request
{
	"size": 0,
	"aggs": {
        "group_by_csn": {
            "terms": {
                "field": "_index",
                "size" : 10000
            }
        }
    }
}
# response
{
	'took': 69,
	'timed_out': False,
	'_shards': {
		'total': 47,
		'successful': 47,
		'skipped': 0,
		'failed': 0
	},
	'hits': {
		'total': 928144,
		'max_score': 0.0,
		'hits': []
	},
	'aggregations': {
		'group_by_csn': {
			'doc_count_error_upper_bound': 0,
			'sum_other_doc_count': 0,
			'buckets': [{
				'key': 'csn33201934',
				'doc_count': 289787
			}, {
				'key': 'csn33213630',
				'doc_count': 163870
			}, {
				'key': 'csn33183137',
				'doc_count': 149570
			}, {
				'key': 'csn33188974',
				'doc_count': 45707
			}, {
				'key': 'csn33167710',
				'doc_count': 21288
			}, {
				'key': 'csn33206588',
				'doc_count': 19556
			}]
		}
	}
}

```

### API Conventions 接口约定
```python 
'''
多数 API 均支持多个 index 的查询，可以使用：
index1,index2,index3 这种简单形式
_all 查询全部 index
*index* 通配符匹配
-index  排除匹配

使用 query string 对index结果集进行控制
ignore_unavailable  true/false 是否忽略不可用的index(不存在或close)
allow_no_indices    true/false 找不到 index 是否报错
expand_wildcards    open/close/none/all 通配符，应用到的index类型

'''
```

### Date math support in index names 日期匹配
```python 
'''
支持索引的日期匹配
<static_name{date_math_expr{date_format|time_zone}}>
其中 < > { } : | / + , 为表达式所需的字符，url中会转义
static_name     静态部分
date_math_expr  动态匹配表达式，可以进行日期运算
date_format     匹配的日期格式
time_zone   is the optional time zone. Defaults to utc.

<logstash-{now/d}> logstash-2024.03.22
<logstash-{now/M}> logstash-2024.03.01
<logstash-{now/M{YYYY.MM}}> logstash-2024.03
<logstash-{now/M-1M{YYYY.MM}}> logstash-2024.02
<logstash-{now/d{YYYY.MM.dd|+12:00}}> logstash-2024.03.23

要在 表达式中使用 { } 等字符，需要转义
<elastic\{ON\}-{now/M}> 匹配 elastic{ON}-2024.03.01
'''
```

### Common options 通用选项
```python 

# Pretty Result 结果美化
'''
适用于 query string
?pretty=true    格式话结果 json
?format=yaml    结果格式化为 yaml
'''

# Human readable 人类可读转换
'''
适用于 query string
?human=true     统计类的字段的结果适合人类读取，1kb 1h
?human=false    统计类的字段的结果适合机器读取, 1024 3600000
'''

# Date Math 日期运算
'''
多数可以接收格式化日期值的参数，可以使用日期计算
比如 gt lt from to

计算表达式以 固定日期 或 now 或 (日期字串 加 ||) 开始，后边拼上计算式
*注意* || 需要转义  \|\|
+1h: Add one hour
-1d: Subtract one day
/d: 取最近的一天

可用的日期
y       Years
M       Months
w       Weeks
d       Days
h       Hours
H       Hours
m       Minutes
s       Seconds

举例 now = 2001-01-01 12:00:00
now+1d = 2001-01-01 13:00:00
now-1d = 2001-01-01 11:00:00
now-1h/d = 2001-01-01 00:00:00  四舍五入至 UTC 的 00:00:00
2001.02.01\|\|+1M/d = 2001-03-01 00:00:00
'''

# Response Filter 结果集筛选
# 所有 Api 均支持对结果集进行筛选
# , 分隔 . 取值
'''
GET /_search?q=elasticsearch&filter_path=took,hits.hits._id,hits.hits._score
{
  "took" : 3,
  "hits" : {
    "hits" : [
      {
        "_id" : "0",
        "_score" : 1.6375021
      }
    ]
  }
}
支持通配符
GET /_cluster/state?filter_path=metadata.indices.*.stat*
{
  "metadata" : {
    "indices" : {
      "twitter": {"state": "open"}
    }
  }
}
支持通配符进行不精确匹配
GET /_cluster/state?filter_path=routing_table.indices.**.state
{
  "routing_table": {
    "indices": {
      "twitter": {
        "shards": {
          "0": [{"state": "STARTED"}, {"state": "UNASSIGNED"}],
          "1": [{"state": "STARTED"}, {"state": "UNASSIGNED"}],
          "2": [{"state": "STARTED"}, {"state": "UNASSIGNED"}],
          "3": [{"state": "STARTED"}, {"state": "UNASSIGNED"}],
          "4": [{"state": "STARTED"}, {"state": "UNASSIGNED"}]
        }
      }
    }
  }
}
也可以选择排除的字段
GET /_count?filter_path=-_shards
{
  "count" : 5
}
包含和不包含可以一起使用
GET /_cluster/state?filter_path=metadata.indices.*.state,-metadata.indices.logstash-*
{
  "metadata" : {
    "indices" : {
      "index-1" : {"state" : "open"},
      "index-2" : {"state" : "open"},
      "index-3" : {"state" : "open"}
    }
  }
}
'''
# Flat Settings 是否按平铺的形式返回
'''
默认 false
GET twitter/_settings?flat_settings=true
{
  "twitter" : {
    "settings": {
      "index.number_of_replicas": "1",
      "index.number_of_shards": "1",
      "index.creation_date": "1474389951325",
      "index.uuid": "n6gzFZTgS664GUfx0Xrpjw",
      "index.version.created": ...,
      "index.provided_name" : "twitter"
    }
  }
}
'''
# parameter 参照下划线大小写约定
# boolean 值需要使用 "true" "false" 
# 数字可以直传或者"123"

# 对于时间设定的参数，时间单位如下
# d h m s ms micros nanos

# 对于空间设置的参数，空间单位如下
# b kb mb gb tb pb

# Distance Units 距离单位， 默认使用 m
'''
Mile            mi or miles
Yard            yd or yards
Feet            ft or feet
Inch            in or inch
Kilometer       km or kilometers
Meter           m or meters
Centimeter      cm or centimeters
Millimeter      mm or millimeters
Nautical mile   NM, nmi, or nauticalmiles
'''
# Fuzziness 模糊匹配
# 有些查询参数可以使用 fuzziness 进行模糊匹配
# 使用的算法是：改动几个字符可以达到完全匹配
# AUTO:[low],[high](默认 AUTO:3,6)
# 意思是 
# 0..2  完全匹配
# 3..5  可以修改1个字符
# >5    可以修改两个字符

# Enabling stack traces 错误追踪
# 默认情况 ES 不返回错误的 stack trace
'''
POST /twitter/_search?size=surprise_me&error_trace=true
{
  "error": {
    "root_cause": [
      {
        "type": "illegal_argument_exception",
        "reason": "Failed to parse int parameter [size] with value [surprise_me]",
        "stack_trace": "Failed to parse int parameter [size] with value [surprise_me]]; nested: IllegalArgumentException..."
      }
    ],
    "type": "illegal_argument_exception",
    "reason": "Failed to parse int parameter [size] with value [surprise_me]",
    "stack_trace": "java.lang.IllegalArgumentException: Failed to parse int parameter [size] with value [surprise_me]\n    at org.elasticsearch.rest.RestRequest.paramAsInt(RestRequest.java:175)...",
    "caused_by": {
      "type": "number_format_exception",
      "reason": "For input string: \"surprise_me\"",
      "stack_trace": "java.lang.NumberFormatException: For input string: \"surprise_me\"\n    at java.lang.NumberFormatException.forInputString(NumberFormatException.java:65)..."
    }
  },
  "status": 400
}
'''
```

