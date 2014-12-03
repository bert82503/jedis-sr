package redis.clients.jedis;

/**
 * "Redis基本命令集"定义。
 * 
 * @author huagang.li 2014年12月3日 下午4:48:39
 */
public interface BasicCommands {

	/*
	 * Connection（连接）
	 */
	String auth(String password);

	String ping();

	String quit();

	String select(int index);

	/*
	 * Server（服务器）
	 */
	String flushDB();

	Long dbSize();

	String flushAll();

	String save();

	String bgsave();

	String bgrewriteaof();

	Long lastsave();

	String shutdown();

	String info();

	String info(String section);

	String slaveof(String host, int port);

	String slaveofNoOne();

	Long getDB();

	String debug(DebugParams params);

	String configResetStat();

	Long waitReplicas(int replicas, long timeout);

}
