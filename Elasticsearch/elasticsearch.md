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