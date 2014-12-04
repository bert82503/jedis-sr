package redis.clients.jedis;

import java.net.URI;

import redis.clients.util.ShardInfo;
import redis.clients.util.Sharded;

/**
 * "Jedis节点分片信息"表示，继承自{@link ShardInfo<Jedis>}。
 * 
 * @author huagang.li 2014年12月2日 下午7:32:23
 */
public class JedisShardInfo extends ShardInfo<Jedis> {

	/** 主机名称/IP */
	private String host;

	/** 端口号 */
	private int port;

	/** 节点的分片名称 */
	private String name;

	/** 访问密码 */
	private String password;

	/** 超时时间 */
	private int timeout;

	public JedisShardInfo(String host) {
		super(Sharded.DEFAULT_WEIGHT);

		URI uri = URI.create(host);
		if (uri.getScheme() != null && uri.getScheme().equals("redis")) { // 验证使用"Redis协议"
			this.host = uri.getHost();
			this.port = uri.getPort();
			this.password = uri.getUserInfo().split(":", 2)[1];
		} else {
			this.host = host;
			this.port = Protocol.DEFAULT_PORT;
		}
	}

	public JedisShardInfo(String host, String name) {
		this(host, Protocol.DEFAULT_PORT, name);
	}

	public JedisShardInfo(String host, int port) {
		this(host, port, Protocol.DEFAULT_TIMEOUT);
	}

	public JedisShardInfo(String host, int port, String name) {
		this(host, port, Protocol.DEFAULT_TIMEOUT, name);
	}

	public JedisShardInfo(String host, int port, int timeout) {
		this(host, port, timeout, Sharded.DEFAULT_WEIGHT);
	}

	/**
	 * 创建一个"Jedis节点分片信息"实例。
	 * 
	 * @param host
	 *            主机名称/IP
	 * @param port
	 *            端口号
	 * @param timeout
	 *            超时时间(ms)
	 * @param name
	 *            节点分片名称
	 */
	public JedisShardInfo(String host, int port, int timeout, String name) {
		this(host, port, timeout, Sharded.DEFAULT_WEIGHT);
		this.name = name;
	}

	/**
	 * 创建一个"Jedis节点分片信息"实例。
	 * 
	 * @param host
	 *            主机名称/IP
	 * @param port
	 *            端口号
	 * @param timeout
	 *            超时时间(ms)
	 * @param weight
	 *            节点权重
	 */
	public JedisShardInfo(String host, int port, int timeout, int weight) {
		super(weight);
		this.host = host;
		this.port = port;
		this.timeout = timeout;
	}

	public JedisShardInfo(URI uri) {
		super(Sharded.DEFAULT_WEIGHT);
		this.host = uri.getHost();
		this.port = uri.getPort();
		this.password = uri.getUserInfo().split(":", 2)[1];
	}

	/**
	 * 创建一个新的"Jedis客户端"实例。
	 */
	@Override
	public Jedis createResource() {
		return new Jedis(this);
	}

	/**
	 * 返回主机信息。
	 */
	public String getHost() {
		return host;
	}

	/**
	 * 返回端口号。
	 */
	public int getPort() {
		return port;
	}

	/**
	 * 返回Redis节点的分片名称。
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * <pre>
	 * 返回格式：
	 * 	主机IP:端口号:权重
	 * </pre>
	 */
	@Override
	public String toString() {
		return host + ":" + port + "*" + this.getWeight();
	}

	public String getPassword() {
		return password;
	}

	/**
	 * 设置访问密码。
	 * 
	 * @param auth
	 */
	public void setPassword(String auth) {
		this.password = auth;
	}

	public int getTimeout() {
		return timeout;
	}

	/**
	 * 设置超时时间。
	 * 
	 * @param timeout
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

}
