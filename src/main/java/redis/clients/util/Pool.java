package redis.clients.util;

import java.io.Closeable;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

/**
 * 连接池抽象表示，基于{@link GenericObjectPool<T>}实现。
 * 
 * @author huagang.li 2014年12月3日 下午7:39:08
 */
public abstract class Pool<R> implements Closeable {

	/** 内部连接池 */
	protected GenericObjectPool<R> internalPool;

	/**
	 * Using this constructor means you have to set and initialize the
	 * internalPool yourself.
	 */
	public Pool() {
	}

	/**
	 * 创建一个"连接池"实例。
	 * 
	 * @param poolConfig
	 *            连接池配置信息
	 * @param factory
	 *            连接池对象的工厂
	 */
	public Pool(GenericObjectPoolConfig poolConfig,
			PooledObjectFactory<R> factory) {
		this.initPool(poolConfig, factory);
	}

	/**
	 * 初始化"连接池"。
	 * 
	 * @param poolConfig
	 *            连接池配置信息
	 * @param factory
	 *            连接池对象的工厂
	 */
	public void initPool(GenericObjectPoolConfig poolConfig,
			PooledObjectFactory<R> factory) {
		// 若原有的连接池资源未被释放，则先关闭
		if (this.internalPool != null) {
			try {
				this.closeInternalPool();
			} catch (Exception e) {
			}
		}

		this.internalPool = new GenericObjectPool<R>(factory, poolConfig);
	}

	/**
	 * 获取"连接池"中的一个资源。
	 */
	public R getResource() {
		try {
			return internalPool.borrowObject();
		} catch (Exception e) {
			// 抛出"无法从连接池中获取到一个资源"的异常
			throw new JedisConnectionException(
					"Could not get a resource from the pool", e);
		}
	}

	/**
	 * 将正常的资源返回给"连接池"。
	 * 
	 * @param resource
	 *            正常的资源
	 */
	public void returnResource(R resource) {
		if (resource != null) {
			this.returnResourceObject(resource);
		}
	}

	/**
	 * 将正常的资源返回给"连接池"。
	 * 
	 * @param resource
	 */
	protected void returnResourceObject(R resource) {
		if (resource == null) {
			return;
		}

		try {
			// 会在getTestOnReturn()打开时，校验资源的有效性
			internalPool.returnObject(resource);
		} catch (Exception e) {
			// 抛出"无法将这个资源返回给连接池"的异常
			throw new JedisException(
					"Could not return the resource to the pool", e);
		}
	}

	/**
	 * 将出现异常的资源返回给"连接池"。
	 * 
	 * @param resource
	 *            出现异常的资源
	 */
	public void returnBrokenResource(R resource) {
		if (resource != null) {
			this.returnBrokenResourceObject(resource);
		}
	}

	/**
	 * 将出现异常的资源返回给"连接池"。
	 * 
	 * @param resource
	 */
	protected void returnBrokenResourceObject(R resource) {
		try {
			// 销毁连接池中的这个资源池对象
			internalPool.invalidateObject(resource);
		} catch (Exception e) {
			// 抛出"无法将这个异常的资源返回给连接池"的异常
			throw new JedisException(
					"Could not return the broken resource to the pool", e);
		}
	}

	/**
	 * 检测"连接池"是否已关闭。
	 */
	public boolean isClosed() {
		return this.internalPool.isClosed();
	}

	/**
	 * 关闭"内部连接池"。
	 */
	protected void closeInternalPool() {
		try {
			internalPool.close();
		} catch (Exception e) {
			// 抛出"无法销毁该连接池"异常
			throw new JedisException("Could not destroy the pool", e);
		}
	}

	/**
	 * 关闭"连接池"资源。
	 */
	@Override
	public void close() {
		this.closeInternalPool();
	}

	/**
	 * 关闭"连接池"资源，与{@link #close()}方法功能一样。
	 */
	public void destroy() {
		this.closeInternalPool();
	}

}
