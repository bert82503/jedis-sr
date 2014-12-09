package redis.clients.util;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "节点分片集群"实现，实现虚拟节点ShardInfo<R>到链接资源R的映射。
 * 
 * @author huagang.li 2014年12月2日 下午8:00:14
 */
public class Sharded<R, S extends ShardInfo<R>> {

	/** 默认的分片权重(虚拟节点数) */
	public static final int DEFAULT_WEIGHT = 1;

	// the tag is anything between {}
	public static final Pattern DEFAULT_KEY_TAG_PATTERN = Pattern
			.compile("\\{(.+?)\\}");

	/*
	 * 集群信息
	 *   "虚拟节点hash值到分片节点的映射表(<hash, ShardInfo<R>>)"
	 *   "分片节点到真实节点链接资源的映射表(<ShardInfo<R>, R>)"
	 * 上述数据结构设计，保证一个"分片集群池对象"只包含分片节点数的真实节点链接资源。
	 * 
	 * 例如，共3台服务器构成一个集群，那么每个池对象就仅包含3条到后端真实节点的链接。
	 */
	/** 虚拟节点hash值到分片节点的映射表(<hash, ShardInfo<R>>) */
	private NavigableMap<Long, S> nodes;
	/** (一致性)哈希算法 */
	private final Hashing algo;
	/** 分片节点到真实节点链接资源的映射表(<ShardInfo<R>, R>) */
	private final Map<S, R> resources = new LinkedHashMap<S, R>();

	/**
	 * The default pattern used for extracting a key tag. The pattern must have
	 * a group (between parenthesis), which delimits the tag to be hashed. A
	 * null pattern avoids applying the regular expression for each lookup,
	 * improving performance a little bit is key tags aren't being used.
	 */
	private Pattern tagPattern = null;

	//
	// ================================
	public Sharded(List<S> shards) {
		// MD5 is really not good as we works with 64-bits not 128
		this(shards, Hashing.MURMUR_HASH);
	}

	public Sharded(List<S> shards, Hashing algo) {
		this.algo = algo;
		this.initialize(shards);
	}

	public Sharded(List<S> shards, Pattern tagPattern) {
		// MD5 is really not good as we works with 64-bits not 128
		this(shards, Hashing.MURMUR_HASH, tagPattern);
	}

	/**
	 * 创建一个"节点分片集群"实例。
	 * 
	 * @param shards
	 *            真实节点列表
	 * @param algo
	 *            哈希算法
	 * @param tagPattern
	 *            键标记模式
	 */
	public Sharded(List<S> shards, Hashing algo, Pattern tagPattern) {
		this.algo = algo;
		this.tagPattern = tagPattern;
		this.initialize(shards);
	}

	/*
	 * [核心] 初始化集群信息。
	 */
	private void initialize(List<S> shards) {
		nodes = new TreeMap<Long, S>();

		int size = shards.size();
		for (int i = 0; i < size; ++i) {
			S shardInfo = shards.get(i);
			
			// 一致性哈希算法
			int weight = 160 * shardInfo.getWeight(); // 放大160倍
			if (shardInfo.getName() == null) {
				for (int n = 0; n < weight; n++) {
					// 构造默认分片名称，并进行哈希计算
					// 大坑：将节点的顺序索引i作为hash的一部分！
					// 当节点顺序被无意识地调整了，那就杯具啦！
					long hash = algo.hash("SHARD-" + i + "-NODE-" + n);
					nodes.put(Long.valueOf(hash), shardInfo);
				}
			} else {
				for (int n = 0; n < weight; n++) {
					// 根据自定义的分片名称和权重来构造分片名称，并进行哈希计算
					// 坑："节点名称+权重"必须是唯一的，否则节点会出现重叠覆盖！
					// 同时，"节点名称+权重"必须不能被中途改变！
					// 这样设计避免了"因节点顺序调整而引发rehash"的问题
					long hash = algo.hash(shardInfo.getName() + "*"
							+ shardInfo.getWeight() + n);
					nodes.put(Long.valueOf(hash), shardInfo);
				}
			}
			// 较好地hash策略是：唯一节点名称+编号
			// long hash = algo.hash(shardInfo.getName() + "*" + n);

			R resource = shardInfo.createResource();
			resources.put(shardInfo, resource);
		}
	}

	/**
	 * 获取给定的键所映射的"节点分片信息"。
	 * 
	 * @param key
	 * @return
	 */
	public S getShardInfo(byte[] key) {
		SortedMap<Long, S> tail = nodes.tailMap(algo.hash(key));
		if (tail.isEmpty()) { // 当定位到末尾节点时，则循环使用第一个节点（构成一个环形）
			return nodes.get(nodes.firstKey());
		}
		return tail.get(tail.firstKey());
	}

	/**
	 * 获取给定的键所映射的"节点分片信息"。
	 * 
	 * @param key
	 *            键
	 * @return
	 */
	public S getShardInfo(String key) {
		return this.getShardInfo(SafeEncoder.encode(this.getKeyTag(key)));
	}

	/**
	 * 获取给定的键所映射的"节点客户端"资源。
	 * 
	 * @param key
	 * @return
	 */
	public R getShard(byte[] key) {
		return resources.get(this.getShardInfo(key));
	}

	/**
	 * 获取给定的键所映射的"节点客户端"资源。
	 * 
	 * @param key
	 *            键
	 * @return
	 */
	public R getShard(String key) {
		return resources.get(this.getShardInfo(key));
	}

	/**
	 * A key tag is a special pattern inside a key that, if preset, is the only
	 * part of the key hashed in order to select the server for this key.
	 * 
	 * @see http://code.google.com/p/redis/wiki/FAQ#I
	 *      'm_using_some_form_of_key_hashing_for_partitioning,_but_wh
	 * @param key
	 * @return The tag if it exists, or the original key
	 */
	public String getKeyTag(String key) {
		if (tagPattern != null) {
			Matcher m = tagPattern.matcher(key);
			if (m.find()) {
				return m.group(1);
			}
		}
		return key;
	}

	/**
	 * 获取所有的"节点分片信息"列表。
	 * 
	 * @return
	 */
	public Collection<S> getAllShardInfo() {
		return Collections.unmodifiableCollection(nodes.values());
	}

	/**
	 * 获取该集群的所有"连接到Redis服务器的链接"资源列表。
	 * 
	 * @return
	 */
	public Collection<R> getAllShards() {
		return Collections.unmodifiableCollection(resources.values());
	}

}
