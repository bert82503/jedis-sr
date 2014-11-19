[![Build Status](https://travis-ci.org/xetorthio/jedis.png?branch=master)](https://travis-ci.org/xetorthio/jedis)

# Jedis (Jedis简介)

Jedis is a blazingly small and sane [Redis](http://github.com/antirez/redis "Redis") java client.
Jedis是一个超级小而稳健的Redis Java客户端

Jedis was conceived to be EASY to use.
Jedis的目标是易于使用

Jedis is fully compatible with redis 2.8.5.
Jedis是完全兼容Redis 2.8.5版本

## Community (社区)

Meet us on IRC: ##jedis on freenode.net

Join the mailing-list at [http://groups.google.com/group/jedis_redis](http://groups.google.com/group/jedis_redis)

## So what can I do with Jedis? (用Jedis可以做什么？)
All of the following redis features are supported:
以下所有的Redis功能都支持：

- Sorting (排序)
- Connection handling (连接处理)
- Commands operating on any kind of values (各种类型的值)
- Commands operating on string values (字符串)
- Commands operating on hashes (哈希表)
- Commands operating on lists (列表)
- Commands operating on sets (集合)
- Commands operating on sorted sets (有序集合)
- Transactions (事务)
- Pipelining (流水线)
- Publish/Subscribe (发布/订阅)
- Persistence control commands (持久化 控制命令)
- Remote server control commands (远程服务器 控制命令)
- Connection pooling (连接池管理)
- Sharding (MD5, MurmurHash) (数据分片)
- Key-tags for sharding
- Sharding with pipelining (流水线分片)
- Scripting with pipelining (流水线脚本)
- Redis Cluster (Redis集群)

## How do I use it? (如何使用它？)

You can download the latest build at: (下载最新版本)
    http://github.com/xetorthio/jedis/releases

Or use it as a maven dependency:

```xml
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>2.4.2</version>
    <type>jar</type>
    <scope>compile</scope>
</dependency>
```

To use it just: (只是使用它)
    
```java
Jedis jedis = new Jedis("localhost");
jedis.set("foo", "bar");
String value = jedis.get("foo");
```

For more usage examples check the tests. (想了解更多使用示例，请查看tests下的模块代码。)

Please check the [wiki](http://github.com/xetorthio/jedis/wiki "wiki"). There are lots of cool things you should know, including information about connection pooling.
请查看wiki文档，那里有很多很酷的东西你应该知道，包括有关连接池管理的信息。

And you are done! (当你已经完成！)

## Jedis Cluster (Jedis集群)

Redis cluster [specification](http://redis.io/topics/cluster-spec) (still under development) is implemented
Redis集群规范（仍在开发中）实现：

```java
Set<HostAndPort> jedisClusterNodes = new HashSet<HostAndPort>();
//Jedis Cluster will attempt to discover cluster nodes automatically (尝试自动发现集群节点)
jedisClusterNodes.add(new HostAndPort("127.0.0.1", 7379));
JedisCluster jc = new JedisCluster(jedisClusterNodes);
jc.set("foo", "bar");
String value = jc.get("foo");
```

## I want to contribute! (我想贡献代码！)

That is great! Just fork the project in github. Create a topic branch, write some code, and add some tests for your new code.

To run the tests:

- Use the latest redis master branch.

- Run ```make test```. This will run 2 instances of redis. We use 2 redis
	servers, one on default port (6379) and the other one on (6380). Both have
	authentication enabled with default password (foobared). This way we can
	test both sharding and auth command. For the Sentinel tests to we use a
	default Sentinel configuration that is configured to properly authenticate
	using the same password with a master called mymaster running on 6379.

Thanks for helping!

## License (许可协议)

Copyright (c) 2011 Jonathan Leibiusky

Permission is hereby granted, free of charge, to any person
obtaining a copy of this software and associated documentation
files (the "Software"), to deal in the Software without
restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following
conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

