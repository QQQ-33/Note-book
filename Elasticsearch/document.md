
## Ducoment Apis 文档操作 

Elasticsearch 将每个 index 分为若干 shards ，每个shards 都可以有多个副本。这些副本组成一个副本组，他们之间必须保持同步。
ES的数据模型基于 primary-backup 模型，Microsoft Research 的论文 PacificA 对这个模型有很好的描述。该模型基于从副本组中获取一个 primary shard，其余的副本作为 copied shards，由 primary shard 负责同步其他 shards，保证数据一致性

#### Basic write model 数据写入

ES中每个对index的操作首先根据 routing 找到副本组，确定副本组后将操作内部转发至 primary shard，primary shard负责将操作转发至其他copied shards。由于copied shards可以offline 所以primary shard无需复制这些操作。ES维护了一张需要操作的copied shards 的列表，称作同步副本列表。顾名思义，它是一组已经处理了所有用户操作的副本列表，primary shard负责维护这个表保持不变，并且将操作发送给这些副本shard

primary shard的工作流程：
- 校验操作
- 在本地执行操作
- 将操作分发至同步副本列表的所有shard，此操作是并发的
- 当所有的副本shard都向primary返回成功，primary向用户返回操作成功

#### Basic read model 数据读取

ES可以进行轻量级查找或者复杂的聚合搜索。基于 primary-backup 模型的好处是所有副本完全相同，这样任意一个副本都可以承担读取请求。当任意node接到读取的请求时，该node将其转发给相关的shard，收集response，并向客户端做出响应。被称为 coordinating node。

read 的流程如下：
- 解析与读取请求相关的shards
- 选择一个 active 的相关的shard，可以时primary或者copy shard，默认从copied shards轮询
- 将读取请求发送至shard
- 合并结果并做出响应

如果读取的过程中部分shard失败，coordinating node将会尝试从其他副本shard读取结果，有可能所有的shard全部读取失败。有些api可以在某些shard读取失败时返回部分的结果：
_search _msearch _bulk _mget

有关更多Elasticsearch是如何工作的请参阅：

[Elasticsearch Resiliency Status](https://www.elastic.co/guide/en/elasticsearch/resiliency/current/index.html)

#### Mapping

在开始 document 的操作之前，需要了解 document 的描述定义 Mapping
 Mapping 用于定义一个文档及其字段如何映射和索引
 
一个 index 只有一个 mapping type 它包含：
- Meta-fields 用于确定document怎么关联元信息
- fields 包含文档的字段属性

Field 的包含很多中数据类型。同一个字段可能用于不同的搜索，比如 full-text search 时应用 text 类型，aggregations 时应用 keyword 类型 。所以大多数的 field datatype 支持 mult-fields，通过设置 field datatype 的 fields 参数可以将同一个字段在不同场景下应用不同的数据类型

过多的 mapping 字段将会造成不可恢复的问题，所以需要对mapping的字段数加以限制：
- index.mapping.total_fields.limit 最多有几个字段，默认1000
- index.mapping.depth.limit 字段嵌套深度，默认20
- index.mapping.nested_fields.limit 不同嵌套映射的数，默认50

如果没有指定 Mapping 字段，在添加document时，ES会自动添加一个mapping。动态mapping的规则可以自定义

不能修改已经存在的mapping，如果要修改应该用新mapping创建一个新index并导入原有index的数据。如果只是想该filed的name，可以尝试用别名来达成。
```python 
PUT my_index
{
  "mappings": {
    "_doc": { 
      "properties": { 
        "title":    { "type": "text"  }, 
        "name":     { "type": "text"  }, 
        "age":      { "type": "integer" },  
        "created":  {
          "type":   "date", 
          "format": "strict_date_optional_time||epoch_millis"
        }
      }
    }
  }
}
```

### Fields Datatypes
```python 
'''
string:
    text and keyword

Numeric datatypes:
    long, integer, short, byte, double, float, half_float, scaled_float

Date datatype:
    date

Boolean datatype:
    boolean

Binary datatype:
    binary

Range datatypes:
    integer_range, float_range, long_range, double_range, date_range, ip_range

Complex datatypesedit:
    Object datatype (object for single JSON objects)
    Nested datatype (nested for arrays of JSON objects)

Geo datatypesedit:
    Geo-point datatype (geo_point for lat/lon points)
    Geo-Shape datatype (geo_shape for complex shapes like polygons)

IP datatype:
    ip for IPv4 and IPv6 addresses

Completion datatype:
    completion to provide auto-complete suggestions

Token count datatype:
    token_count to count the number of tokens in a string

mapper-murmur3:
    murmur3 to compute hashes of values at index-time and store them in the index

mapper-annotated-text:
    annotated-text to index text containing special markup (typically used for identifying named entities)

Percolator type:
    Accepts queries from the query-dsl

join datatype:
    Defines parent/child relation for documents within the same index

Alias datatype:
    Defines an alias to an existing field.

Arrays:
    ES中，任何字段都可以设为array类型，但是同一个字段包含的所有元素必须是同一类型

Multi-fields:
    一个经常出现的场景，对于同一个字段在不同的搜索情况下需要应用不同的类型，必须全文检索时需要应用 text 类型，聚合搜索时需要应用 keyword 类型。或者对同一个字段应用不同的分析器。
    通过 fields 属性来进行设置。
'''
```
#### Alias datatype
```python 
# alias 只能应用于单 type 的index中
# 别名可以用来代替原字段进行搜索，所以如果只是想修改mapping中的字段名，可以使用 alias
PUT trips
{
  "mappings": {
    "_doc": {
      "properties": {
        "distance": { # 原字段
          "type": "long"
        },
        "route_length_miles": { # 别名字段
          "type": "alias", # 这里类型选择 alias
          "path": "distance" # 指向 distance 字段，必须是精确路径
        },
        "transit_mode": {
          "type": "keyword"
        }
      }
    }
  }
}

```
#### Arrays 
```python 
# array 不是一个类型，当向同一个文档的同一个字段添加多个 value 时，它自动变为 array，同一个字段只能包含相同类型的数据
# array 类型和通常的 array不同，不能查询每个对象，如果想查询每个对象，应该使用 nested 类型
PUT my_index/_doc/1 # 一个包含数组类型的文档
{
  "message": "some arrays in this document...",
  "tags":  [ "elasticsearch", "wow" ], # 无需配置，直接使用即可
  "lists": [ 
    {
      "name": "prog_list",
      "description": "programming list"
    },
    {
      "name": "cool_list",
      "description": "cool stuff list"
    }
  ]
}
```
#### Binary 
```python 
# binary 类型可以保存base64加密的字串，且不可以 search
# 可选参数：
# doc_values    true/false 是否以 column-stride 方式保存在磁盘，比便后续的查询，聚合等操作
# store     true/false 是否单独保存，以便从 _source 以外的方式获取字段的原始值
PUT my_index
{
  "mappings": {
    "_doc": {
      "properties": {
        "name": {
          "type": "text"
        },
        "blob": {
          "type": "binary"
        }
      }
    }
  }
}

PUT my_index/_doc/1
{
  "name": "Some binary blob",
  "blob": "U29tZSBiaW5hcnkgYmxvYg==" 
}
```
#### Range 
```python 
# 
PUT range_index
{
  "settings": {
    "number_of_shards": 2
  },
  "mappings": {
    "_doc": {
      "properties": {
        "expected_attendees": {
          "type": "integer_range"
        },
        "time_frame": {
          "type": "date_range", 
          "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"
        }
      }
    }
  }
}

PUT range_index/_doc/1?refresh
{
  "expected_attendees" : { 
    "gte" : 10,
    "lte" : 20
  },
  "time_frame" : { 
    "gte" : "2015-10-31 12:00:00", 
    "lte" : "2015-11-01"
  }
}
```
#### Date 
```python 
# 日期类型，支持多种格式化
# 由于 json 没有date类型，所以ES里date类型可以保存日期字串，或者一个long值(毫秒)，或int值(秒)
# ES内部会将日期保存为long值(毫秒数)
# 查询的时候转换为long值范围的查询，查询的结果再根据format转回string。
# 日期总是以string的形式展示
# 默认的 format ："strict_date_optional_time||epoch_millis"
PUT my_index
{
  "mappings": {
    "_doc": {
      "properties": {
        "date": {
          "type": "date"，
          "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"
        }
      }
    }
  }
}

PUT my_index/_doc/1
{ "date": "2015-01-01" } 

PUT my_index/_doc/2
{ "date": "2015-01-01T12:10:30Z" } 

PUT my_index/_doc/3
{ "date": 1420070400001 } 

GET my_index/_search
{
  "sort": { "date": "asc"} 
}
```
#### Geo-point 
```python 
# 坐标点类型，保存经纬度 lat lon
# 展示顺序：字串形式： lat lon 数组形式：[lon, lat]  
PUT my_index
{
  "mappings": {
    "_doc": {
      "properties": {
        "location": {
          "type": "geo_point"
        }
      }
    }
  }
}

PUT my_index/_doc/1
{
  "text": "Geo-point as an object",
  "location": { 
    "lat": 41.12,
    "lon": -71.34
  }
}

PUT my_index/_doc/2
{
  "text": "Geo-point as a string",
  "location": "41.12,-71.34" 
}

PUT my_index/_doc/3
{
  "text": "Geo-point as a geohash",
  "location": "drm3btev3e86" 
}

PUT my_index/_doc/4
{
  "text": "Geo-point as an array",
  "location": [ -71.34, 41.12 ] 
}

GET my_index/_search
{
  "query": {
    "geo_bounding_box": { 
      "location": {
        "top_left": {
          "lat": 42,
          "lon": -72
        },
        "bottom_right": {
          "lat": 40,
          "lon": -74
        }
      }
    }
  }
}
```
#### Geo-shap
```python 
# 地理图形，当需要查询一个地理坐标图形时，可以使用
# 重要参数：
# orientation   如何定义多边形坐标点的描绘顺序。有两种方式：
#   right,ccw,counterclockwise 逆时针
#   left,cw,clockwise   顺时针
# 默认采用 OGC 标准，外圈坐标逆时针，内圈坐标顺时针
# 可以整体定义，也可以在 GeoJson 或 WKT 中定义
# coerce    是否自动闭合多边形，默认 false
PUT /example
{
    "mappings": {
        "doc": {
            "properties": {
                "location": {
                    "type": "geo_shape"# 定义 geo_shape 类型
                }
            }
        }
    }
}
# 图形的输入，可以输入多种类型的图形
# 可以使用 GeoJson 或者 WKT (Well-Know Text)对多边形进行描绘
# 需要注意，有三种 type 可能会用到：
# GeoJson       WKT         Elasticsearch   每种类型的单词略有差别
# +--------------------------------------------------------------+
# Point         POINT       point           只有一个坐标点的类型
# LineString    LINESTRING  linestring      多个坐标组成的折线
# Polygon       POLYGON     polygon         闭合多边形，第一个点和最后一个点需要重合，最少需要4个坐标
# MultiPoint    MULTIPOINT  multipoint      一组看上去有关联但是不连接的点
# MultiLineString   MULTILINESTRING multilinestring 一组折线
# MultiPolygon  MULTIPOLYGON    multipolygon    一组多边形
# GeometryCollection    GEOMETRYCOLLECTION  geometrycollection  一组混合形状(不同形状的组合)
# N/A   BBOX    envelope    通过指定左上角和右下角确定的矩形
# N/A   N/A     circle      通过指定中心点和半径确定的原型，半径单位默认m
# 以上所有形状在输入时都需要指定 type 和 coordinates 属性
# 输入坐标时的顺序是 X, Y (lon, lat: 经度, 维度)。
# 各类型输入的例子：
# Point
POST /example/doc
{
    "location" : {
        "type" : "point",
        "coordinates" : [-77.03653, 38.897676]
    }
}
# linestring
POST /example/doc
{
    "location" : {
        "type" : "linestring",
        "coordinates" : [[-77.03653, 38.897676], [-77.009051, 38.889939]] # 最少2个点
    }
}
# polygon 
# 注意描绘图形时坐标点的描绘顺序
POST /example/doc
{
    "location" : {
        "type" : "polygon",
        "coordinates" : [
            [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ]# 第一个点和最后一个点需要重合
        ]
    }
}
POST /example/doc # 在图形中开洞
{
    "location" : {
        "type" : "polygon",
        "coordinates" : [
            [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ],# 第一组为外圈
            [ [100.2, 0.2], [100.8, 0.2], [100.8, 0.8], [100.2, 0.8], [100.2, 0.2] ]# 后边的组为内圈
        ]
    }
}
# MultiPoint
POST /example/doc # 二维数组
{
    "location" : {
        "type" : "multipoint",
        "coordinates" : [
            [102.0, 2.0], [103.0, 2.0]
        ]
    }
}
# MultiLineString
POST /example/doc # 三维数组
{
    "location" : {
        "type" : "multilinestring",
        "coordinates" : [
            [ [102.0, 2.0], [103.0, 2.0], [103.0, 3.0], [102.0, 3.0] ],
            [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0] ],
            [ [100.2, 0.2], [100.8, 0.2], [100.8, 0.8], [100.2, 0.8] ]
        ]
    }
}
# MultiPolygon
POST /example/doc # 四维数组
{
    "location" : {
        "type" : "multipolygon",
        "coordinates" : [
            [ [[102.0, 2.0], [103.0, 2.0], [103.0, 3.0], [102.0, 3.0], [102.0, 2.0]] ],
            [ [[100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0]],
              [[100.2, 0.2], [100.8, 0.2], [100.8, 0.8], [100.2, 0.8], [100.2, 0.2]] ]
        ]
    }
}
# Geometry Collection
POST /example/doc
{
    "location" : {
        "type": "geometrycollection",
        "geometries": [ # 对每个形状单独定义
            {
                "type": "point",
                "coordinates": [100.0, 0.0]
            },
            {
                "type": "linestring",
                "coordinates": [ [101.0, 0.0], [102.0, 1.0] ]
            }
        ]
    }
}
# envelope 
POST /example/doc
{
    "location" : {
        "type" : "envelope",
        "coordinates" : [ [100.0, 1.0], [101.0, 0.0] ] # 只需要定义左上角及右下角坐标
    }
}
# Circle
POST /example/doc
{
    "location" : {
        "type" : "circle",
        "coordinates" : [101.0, 1.0],
        "radius" : "100m"
    }
}

# geo_shape 类型的字段目前无法排序和直接获取，只能通过 _source 获取
```
#### Keyword 
```python 
# 关键字类型用于保存结构化的值，比如 ip email hostname等
# 通常用于过滤，聚合，排序等
# 只能按实际值查询，不能进行全文检索。
PUT my_index
{
  "mappings": {
    "_doc": {
      "properties": {
        "tags": {
          "type":  "keyword"
        }
      }
    }
  }
}
# 关键参数：
# ignore_above  可索引字段值的最大长度，默认为 2147483647 但是动态mapping会创建一个覆盖这个值为 256
# 
```
#### Object 
```python 
# json 文档类型
# ES内部会将 json 的结构展开来进行索引
PUT my_index/_doc/1
{ 
  "region": "US",
  "manager": { 
    "age":     30,
    "name": { 
      "first": "John",
      "last":  "Smith"
    }
  }
}
# ES内部：
{
  "region":             "US",
  "manager.age":        30,
  "manager.name.first": "John",
  "manager.name.last":  "Smith"
}
# 上面的例子中 object 的内部结构由ES自动 mapping，当然可以对所有内部字段指定类型
PUT my_index
{
  "mappings": {
    "_doc": { 
      "properties": {
        "region": {
          "type": "keyword"
        },
        "manager": { # 不用显示的指定 type: object， ES自动识别
          "properties": {
            "age":  { "type": "integer" },
            "name": { 
              "properties": {
                "first": { "type": "text" },
                "last":  { "type": "text" }
              }
            }
          }
        }
      }
    }
  }
}

```
#### Nested 
```python 
# Object的数组类型
# 它不同于 Object array，数组中的每个Object都将被索引，而Object array将会被展开，所以如果需要对数组中的那个元素进行单独索引，应该用 nested 类型，不应该用 object 类型
PUT my_index
{
  "mappings": {
    "_doc": {
      "properties": {
        "user": {
          "type": "nested" 
        }
      }
    }
  }
}

PUT my_index/_doc/1
{
  "group" : "fans",
  "user" : [
    {
      "first" : "John",
      "last" :  "Smith"
    },
    {
      "first" : "Alice",
      "last" :  "White"
    }
  ]
}

GET my_index/_search
{
  "query": {
    "nested": {
      "path": "user",
      "query": {
        "bool": {
          "must": [
            { "match": { "user.first": "Alice" }},
            { "match": { "user.last":  "Smith" }} 
          ]
        }
      }
    }
  }
}

GET my_index/_search
{
  "query": {
    "nested": {
      "path": "user",
      "query": {
        "bool": {
          "must": [
            { "match": { "user.first": "Alice" }},
            { "match": { "user.last":  "White" }} 
          ]
        }
      },
      "inner_hits": { 
        "highlight": {
          "fields": {
            "user.first": {}
          }
        }
      }
    }
  }
}
# 嵌套类型的字段可以使用 嵌套类型的查询，聚合，排序，还可以使用 inner hits
# 关键参数：
# properties    用于描述 object 以及 nested 类型的数据类型，可以继续嵌套
```
#### Numberic 
```python 
# 数字类型，具体包含如下类型：
# long integer short byte double float half_float scaled_float
# 对于浮点类型的字段，-0.0 和 +0.0 代表不同的值，需要区分对待
PUT my_index
{
  "mappings": {
    "_doc": {
      "properties": {
        "number_of_bytes": {
          "type": "integer"
        },
        "time_in_seconds": {
          "type": "float"
        },
        "price": {
          "type": "scaled_float",
          "scaling_factor": 100
        }
      }
    }
  }
}
# 对于 scaled_float 类型，需要额外指定 scaling_factor 参数，这个参数决定了 scaled_float 的缩放程度。
# 例如 scaling_factor = 2.34 scaled_float = 10 ，在ES内部会将rount(2.34 * 10) = 23 (计算结果以long保存)这个值保存到index。在进行搜索时相当于这个文档的这个字段的值是2.3。
# 所以 scaling_factor 的值越大，精度越高。

```
#### Text 
```python 
# 全文检索类型，适合于文章的内容等。
# 对比 keywork ，keywork 只能使用完整的值进行比较，适合进行过滤/排序/聚合，不能进行全文检索。text 类型适合进行全文检索，不能用于排序，很少用于聚合
# text 字段经过 analyzer 的分词处理
PUT my_index
{
  "mappings": {
    "_doc": {
      "properties": {
        "full_name": {
          "type":  "text"
        }
      }
    }
  }
}
# 重要参数：
# analyzer 分词器，分词器会在创建index时/对字段搜索时对内容进行分词，可以通过添加分词器对中文进行分词。
# index_options 索引选项，对于一个字段的索引保存到哪个级别，一般的字段只需要保存 文档id，如果需要，还可以保存到 term 在文档中的位置等信息。对于 text 类型，默认启用position，其他类型默认 doc 
# index_prefixes    如果启用，一个 term的前 2-5 个字符将单独分出一个字段，这会使perfix搜索更有效率，但会花费更多代价
# index_phrases     如果启用，双词组合将会index成一个单独字段
# position_increment_gap    analyzer 的分词粒度，防止过大的分词
# similarity    评分算法，默认 BM25。对文档的全文检索的匹配评分算法
# term_vector   索引信息，比如首字符和末字符处于文档中的位置
```
#### Token count
```python 
# 其实是一个integer类型，他接受字串值，然后解析成term，将解析出的term数保存起来
PUT my_index
{
  "mappings": {
    "_doc": {
      "properties": {
        "name": { 
          "type": "text",
          "fields": {
            "length": { # name 字段的子类
              "type":     "token_count",
              "analyzer": "standard"
            }
          }
        }
      }
    }
  }
}

PUT my_index/_doc/1
{ "name": "John Smith" }

PUT my_index/_doc/2
{ "name": "Rachel Alice Williams" }

GET my_index/_search
{
  "query": {
    "term": {
      "name.length": 3 # 查询由三个部分组成的name
    }
  }
}

```
#### join
```python 
# 用于在同一个document中创建父/子关系
# 它会将两个 document 分开处理，但是保持了他们之间的关系
# parent -> children 为一对多
PUT my_index
{
  "mappings": {
    "_doc": {
      "properties": {
        "my_join_field": { 
          "type": "join",
          "relations": {
            "question": "answer" # question 是 answer 的父
          }
        }
      }
    }
  }
}
# 创建 document 时，对于 join 类型的字段，需要指明保存的是 parent 还是 child
PUT my_index/_doc/1?refresh # 注意这里指定了 document 的id，也就是parent的id，并且通过 refresh 强制刷新了 index
{
  "text": "This is a question",
  "my_join_field": { # name 属性必须指定
    "name": "question" # 这里指明了是 question 也就是 parent
  }
}
# 向父中添加子
PUT my_index/_doc/3?routing=1&refresh # 必须指定 routing，因为父子必须在一个shard上
{
  "text": "This is an answer",
  "my_join_field": { # name 属性必须指定
    "name": "answer", 
    "parent": "1" # 指定父 document 的 id
  }
}
# ES 中的数据应尽量去规范化。
# join 类型会带来额外的开销，除非数据包含一个 one to many 的关系，尽量不要使用它。
# 每个 index 中只允许一个 join 字段。
# parent/child document 必须在同一个 shard 上。也就是 get/update/delete 的时候需要指定同一个 routing。
# 只能有一个 parent， 但可以有多个 child
```
#### Meta-Fields
```python 
# 元信息。每个 document 都包含自己的元信息。
#######
_field_names 用于索引所有非空的字段。它被用于 exists 查询，搜索所有具有非空的特定字段的 document
_field_names 仅用于 disabled doc_value 和 norms 的字段，对于 enabled doc_value 和 norms 的字段， exists 查询依然可用，但是将不会通过 _field_names 去查找。
除非必要，否则不要禁用 _field_names 
PUT tweets
{
  "mappings": {
    "_doc": {
      "_field_names": {
        "enabled": false # 禁用 _field_names
      }
    }
  }
}
#######
_ignored 用于保存所有由于格式错误且开启了 ignore_malformed 的字段。
#######
_id 每个 document 都包含一个 _id ，用于 query sort
#######
_index 在跨多个 index 的查询中，可以用来进行对 index 的筛选。
_index 可以用于多种查询：
GET index_1,index_2/_search
{
  "query": {
    "terms": {
      "_index": ["index_1", "index_2"] # 查询
    }
  },
  "aggs": {
    "indices": {
      "terms": {
        "field": "_index", # 相同index进行聚合
        "size": 10
      }
    }
  },
  "sort": [
    {
      "_index": { # 使用 _index 进行排序
        "order": "asc"
      }
    }
  ],
  "script_fields": {
    "index_name": {
      "script": {
        "lang": "painless",
        "source": "doc['_index']" # 获取 _index 值
      }
    }
  }
}
#######
_routing 字段用于指定路由到哪个shard
指定的shard的算法：
  shard_num = hash(_routing) % num_primary_shards
默认的 routing 是 document 的id，当然在创建 document 时可以指定 routing
PUT my_index/_doc/1?routing=user1&refresh=true 
{ # 这里 user1 被指定为 routing
  "title": "This is a document"
}
# 查询的时候可以指定 routing
GET my_index/_doc/1?routing=user1 
GET my_index/_search
{
  "query": {
    "terms": {
      "_routing": [ "user1" ] 
    }
  }
}
# 使用带有 routing 的查询，可以减少查询时影响的shard。
# 对于使用 routing 的的文档，增删改查时都需要指定 routing，如果忘记，那么这个文档会被在多个shards 上进行索引。可以设定 _routing 为必需的。
PUT my_index2
{
  "mappings": {
    "_doc": {
      "_routing": {
        "required": true # 必须指定 routing
      }
    }
  }
}
# 使用 routing 进行索引时，document 的id不保证唯一性，不同的shard上可能存在相同id的document，所以需要用户自己保证id的唯一性。
# 通过设置 index.routing_partition_size 可以将相同 routing 的document传入不同的shard，这可以减少数据倾斜，降低搜索的影响。
# 当使用 index.routing_partition_size 时，routing到的shard数公式变为：
shard_num = (hash(_routing) + hash(_id) % routing_partition_size) % num_primary_shards
# 可以看到，将 id 作为了计算参数
# 大于 1 ，小于分片数
#  1 < index.routing_partition_size < index.number_of_shards

#######
_source 这个字段保存了 document 的原始值，禁用 _source 字段会使很多功能不可用。
如果 硬盘资源确实紧张，可以考虑先增加  compression level 。
可以指定哪些字段不保存，也就是从 _source 中移除，但是依然可以查询
PUT logs
{
  "mappings": {
    "_doc": {
      "_source": {
        "includes": [
          "*.count", # 字段支持 通配符
          "meta.*"
        ],
        "excludes": [
          "meta.description",
          "meta.other.*"
        ]
      }
    }
  }
}
```


**所有 CRUD 操作仅针对单 index**
#### Single document Apis
```python 

```


#### Multi document Apis
```python 


```
