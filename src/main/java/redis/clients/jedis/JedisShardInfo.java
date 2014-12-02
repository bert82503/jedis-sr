package redis.clients.jedis;

import java.net.URI;

import redis.clients.util.ShardInfo;
import redis.clients.util.Sharded;

/**
 * "Redis节点分片信息"表示。(继承自 ShardInfo<Jedis>)
 * 
 * @author huagang.li 2014年12月2日 下午7:32:23
 */
public class JedisShardInfo extends ShardInfo<Jedis> {

	/** 主机[名称/IP] */
	private String host;

	/** 端口号 */
	private int port;

	/** 节点名称 */
	private String name;

	/** 访问密码 */
	private String password;

	/** 超时时间控制 */
	private int timeout;

	/**
	 * 创建一个使用给定主机的"Redis节点分片信息"实例。
	 * 
	 * @param host
	 */
	public JedisShardInfo(String host) {
		super(Sharded.DEFAULT_WEIGHT);

		URI uri = URI.create(host);
		if (uri.getScheme() != null && uri.getScheme().equals("redis")) { // 验证使用Redis协议
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
		this(host, port, 2000);
	}

	public JedisShardInfo(String host, int port, String name) {
		this(host, port, 2000, name);
	}

	public JedisShardInfo(String host, int port, int timeout) {
		this(host, port, timeout, Sharded.DEFAULT_WEIGHT);
	}

	public JedisShardInfo(String host, int port, int timeout, String name) {
		this(host, port, timeout, Sharded.DEFAULT_WEIGHT);
		this.name = name;
	}

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
	 * <pre>
	 * 返回格式：
	 * 	主机IP:端口号:权重
	 * </pre>
	 */
	@Override
	public String toString() {
		return host + ":" + port + "*" + this.getWeight();
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String auth) {
		this.password = auth;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public String getName() {
		return name;
	}

	@Override
	public Jedis createResource() {
		return new Jedis(this);
	}

}
