### ES
> Elasticsearch 是一个分布式的搜索和分析引擎
> 提供近实时的搜索和分析，可以保存各种类型的数据，结构化，非结构化，数字，地理信息。
> ES 以文档形式保存复杂的数据结构，以JSON格式序列化。当ES以集群形式存在，document存在于各个节点，可以从任意节点快速访问。
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