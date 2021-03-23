### 配置 ES

$ES_HOME/config

- elasticsearch.yml for configuring Elasticsearch
- jvm.options for configuring Elasticsearch JVM settings
- log4j2.properties for configuring Elasticsearch logging

### 重要配置

```yaml 
# 最好在 prod 修改这两个路径，以便未来的 ES 升级等操作
path:
  logs: /var/log/elasticsearch # 日志位置
  data: /var/data/elasticsearch # 数据存放位置

# data 路径可以设置多个，每个路径都将被使用，但同一个shard的数据放在同一个路径下
path:
  data:
    - /mnt/elasticsearch_1
    - /mnt/elasticsearch_2
    - /mnt/elasticsearch_3
```

```yaml 
# 集群名称，一个 node 只能属于一个 cluster。 不同的集群间需要使用不同的 cluster.name
cluster.name: logging-prod
```

```yaml 
# 节点名称
node.name: prod-data-2

# 可以使用环境变量， 比如 hostname
node.name: ${HOSTNAME}
```

```yaml 
# 节点的 ip， 默认是 127.0.0.1 [::1]
network.host: 192.168.1.10
```

```yaml 
# ES 使用 Zen Discovery 进行集群的管理
# 指定一个集群可能出现的节点 hsot 或者 ip
discovery.zen.ping.unicast.hosts:
   - 192.168.1.10:9300
   - 192.168.1.11 
   - seeds.mydomain.com 
# 最小集群节点数，用于防止脑裂
# 总节点数/2 + 1
discovery.zen.minimum_master_nodes: 2   
```

```yaml 
# 堆内存大小
# Xms Xmx 最好相等
# 尽量不超过物理内存的 50%
# 不要超过 JVM 压缩指针的最大内存限制
-Xms2g 
-Xmx2g 
```