package redis.clients.jedis;

import java.net.URI;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.util.JedisURIHelper;
import redis.clients.util.Pool;

/**
 * "Jedis连接池"实现，继承自{@link Pool<Jedis>}。
 * 
 * @author huagang.li 2014年12月9日 下午8:03:48
 */
public class JedisPool extends Pool<Jedis> {

	public JedisPool(GenericObjectPoolConfig poolConfig, String host) {
		this(poolConfig, host, Protocol.DEFAULT_PORT, Protocol.DEFAULT_TIMEOUT,
				null, Protocol.DEFAULT_DATABASE, null);
	}

	public JedisPool(String host, int port) {
		this(new GenericObjectPoolConfig(), host, port,
				Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE, null);
	}

	public JedisPool(String host) {
		URI uri = URI.create(host);
		if (uri.getScheme() != null && uri.getScheme().equals("redis")) {
			String h = uri.getHost();
			int port = uri.getPort();
			String password = JedisURIHelper.getPassword(uri);
			int database = 0;
			Integer dbIndex = JedisURIHelper.getDBIndex(uri);
			if (dbIndex != null) {
				database = dbIndex.intValue();
			}
			this.internalPool = new GenericObjectPool<Jedis>(
					new JedisFactory(h, port, Protocol.DEFAULT_TIMEOUT,
							password, database, null),
					new GenericObjectPoolConfig());
		} else {
			this.internalPool = new GenericObjectPool<Jedis>(new JedisFactory(
					host, Protocol.DEFAULT_PORT, Protocol.DEFAULT_TIMEOUT,
					null, Protocol.DEFAULT_DATABASE, null),
					new GenericObjectPoolConfig());
		}
	}

	public JedisPool(URI uri) {
		this(new GenericObjectPoolConfig(), uri, Protocol.DEFAULT_TIMEOUT);
	}

	public JedisPool(URI uri, int timeout) {
		this(new GenericObjectPoolConfig(), uri, timeout);
	}

	public JedisPool(GenericObjectPoolConfig poolConfig,
			String host, int port, int timeout, String password) {
		this(poolConfig, host, port, timeout, password,
				Protocol.DEFAULT_DATABASE, null);
	}

	public JedisPool(GenericObjectPoolConfig poolConfig,
			String host, int port) {
		this(poolConfig, host, port, Protocol.DEFAULT_TIMEOUT, null,
				Protocol.DEFAULT_DATABASE, null);
	}

	public JedisPool(GenericObjectPoolConfig poolConfig,
			String host, int port, int timeout) {
		this(poolConfig, host, port, timeout, null, Protocol.DEFAULT_DATABASE,
				null);
	}

	public JedisPool(GenericObjectPoolConfig poolConfig,
			String host, int port, int timeout, String password,
			int database) {
		this(poolConfig, host, port, timeout, password, database, null);
	}

	public JedisPool(GenericObjectPoolConfig poolConfig,
			String host, int port, int timeout, String password,
			int database, String clientName) {
		super(poolConfig, new JedisFactory(host, port, timeout, password,
				database, clientName));
	}

	public JedisPool(GenericObjectPoolConfig poolConfig, URI uri) {
		this(poolConfig, uri, Protocol.DEFAULT_TIMEOUT);
	}

	public JedisPool(GenericObjectPoolConfig poolConfig, URI uri,
			int timeout) {
		super(poolConfig, new JedisFactory(uri.getHost(), uri.getPort(),
				timeout, JedisURIHelper.getPassword(uri),
				JedisURIHelper.getDBIndex(uri) != null ? JedisURIHelper
						.getDBIndex(uri) : 0, null));
	}

	@Override
	public Jedis getResource() {
		Jedis jedis = super.getResource();
		jedis.setDataSource(this);
		return jedis;
	}

	@Override
	public void returnResource(Jedis resource) {
		if (resource != null) {
			resource.resetState();
			super.returnResourceObject(resource);
		}
	}

	@Override
	public void returnBrokenResource(Jedis resource) {
		if (resource != null) {
			super.returnBrokenResourceObject(resource);
		}
	}

	public int getNumActive() {
		if (this.internalPool == null || this.internalPool.isClosed()) {
			return -1;
		}
		return this.internalPool.getNumActive();
	}

}
