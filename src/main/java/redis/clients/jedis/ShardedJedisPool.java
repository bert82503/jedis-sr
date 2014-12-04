package redis.clients.jedis;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.util.Hashing;
import redis.clients.util.Pool;

/**
 * "数据分片的Jedis连接池"实现，继承自{@link Pool<ShardedJedis>}。
 * 
 * @author huagang.li 2014年12月2日 下午7:29:59
 */
public class ShardedJedisPool extends Pool<ShardedJedis> {

	/**
	 * 创建一个"数据分片的Jedis连接池"实例。
	 * 
	 * @param poolConfig
	 *            连接池配置信息
	 * @param shards
	 *            Jedis节点分片信息列表
	 */
	public ShardedJedisPool(GenericObjectPoolConfig poolConfig,
			List<JedisShardInfo> shards) {
		this(poolConfig, shards, Hashing.MURMUR_HASH);
	}

	public ShardedJedisPool(GenericObjectPoolConfig poolConfig,
			List<JedisShardInfo> shards, Hashing algo) {
		this(poolConfig, shards, algo, null);
	}

	public ShardedJedisPool(GenericObjectPoolConfig poolConfig,
			List<JedisShardInfo> shards, Pattern keyTagPattern) {
		this(poolConfig, shards, Hashing.MURMUR_HASH, keyTagPattern);
	}

	/**
	 * 创建一个"数据分片的Jedis连接池"实例，使用自定义实现的{@link ShardedJedisFactory}。
	 * 
	 * @param poolConfig
	 *            连接池配置信息
	 * @param shards
	 *            Jedis节点分片信息列表
	 * @param algo
	 *            哈希算法
	 * @param keyTagPattern
	 *            键标记模式
	 */
	public ShardedJedisPool(GenericObjectPoolConfig poolConfig,
			List<JedisShardInfo> shards, Hashing algo, Pattern keyTagPattern) {
		super(poolConfig, new ShardedJedisFactory(shards, algo, keyTagPattern));
	}

	/**
	 * 获取"Jedis连接池"中的一个{@link ShardedJedis}资源。
	 * 
	 * <pre>
	 * 分2个步骤：
	 * 	1. 从Pool<ShardedJedis>中获取一个{@link ShardedJedis}资源；
	 * 	2. 设置{@link ShardedJedis}资源所在的连接池数据源。
	 * </pre>
	 */
	@Override
	public ShardedJedis getResource() {
		ShardedJedis jedis = super.getResource();
		jedis.setDataSource(this);
		return jedis;
	}

	/**
	 * 将正常的{@link ShardedJedis}资源返回给"连接池"。
	 */
	@Override
	public void returnResource(ShardedJedis resource) {
		if (resource != null) {
			resource.resetState();
			this.returnResourceObject(resource);
		}
	}

	/**
	 * 将出现异常的{@link ShardedJedis}资源返回给"连接池"。
	 */
	@Override
	public void returnBrokenResource(ShardedJedis resource) {
		if (resource != null) {
			this.returnBrokenResourceObject(resource);
		}
	}

	/**
	 * PoolableObjectFactory custom impl.
	 * <p>
	 * {@link PooledObjectFactory<ShardedJedis>}自定义实现类。
	 */
	private static class ShardedJedisFactory implements
			PooledObjectFactory<ShardedJedis> {

		/** Jedis节点分片列表 */
		private List<JedisShardInfo> shards;
		/** (一致性)哈希算法 */
		private Hashing algo;
		/** 键标记模式 */
		private Pattern keyTagPattern;

		public ShardedJedisFactory(List<JedisShardInfo> shards, Hashing algo,
				Pattern keyTagPattern) {
			this.shards = shards;
			this.algo = algo;
			this.keyTagPattern = keyTagPattern;
		}

		/**
		 * 创建一个{@link ShardedJedis}资源实例，并将它包装在{@link PooledObject}里便于连接池管理。
		 * <p>
		 * {@inheritDoc}
		 */
		@Override
		public PooledObject<ShardedJedis> makeObject() throws Exception {
			ShardedJedis jedis = new ShardedJedis(shards, algo, keyTagPattern);
			return new DefaultPooledObject<ShardedJedis>(jedis);
		}

		/**
		 * 销毁整个{@link ShardedJedis}资源连接池。
		 * <p>
		 * {@inheritDoc}
		 */
		@Override
		public void destroyObject(PooledObject<ShardedJedis> pooledShardedJedis)
				throws Exception {
			final ShardedJedis shardedJedis = pooledShardedJedis.getObject();
			shardedJedis.disconnect();
			// for (Jedis jedis : shardedJedis.getAllShards()) {
			// try {
			// try {
			// // 请求服务端关闭连接
			// jedis.quit();
			// } catch (Exception e) {
			// // 忽略
			// }
			// // 客户端主动关闭连接
			// jedis.disconnect();
			// } catch (Exception e) {
			//
			// }
			// }
		}

		/**
		 * 校验整个{@link ShardedJedis}资源连接池中的所有是Jedis客户端链接否正常。
		 * <p>
		 * {@inheritDoc}
		 */
		@Override
		public boolean validateObject(
				PooledObject<ShardedJedis> pooledShardedJedis) {
			try {
				ShardedJedis jedis = pooledShardedJedis.getObject();
				for (Jedis shard : jedis.getAllShards()) {
					if (!shard.ping().equals("PONG")) { // PING 命令
						return false;
					}
				}
				return true;
			} catch (Exception ex) {
				return false;
			}
		}

		@Override
		public void activateObject(PooledObject<ShardedJedis> p)
				throws Exception {
			//
		}

		@Override
		public void passivateObject(PooledObject<ShardedJedis> p)
				throws Exception {
			//
		}

	}

}