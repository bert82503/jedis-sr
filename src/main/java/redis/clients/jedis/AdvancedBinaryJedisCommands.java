package redis.clients.jedis;

import java.util.List;

/**
 * 高级的"Redis命令集"定义。
 * 
 * @author huagang.li 2014年12月3日 下午5:10:25
 */
public interface AdvancedBinaryJedisCommands {

	List<byte[]> configGet(byte[] pattern);

	byte[] configSet(byte[] parameter, byte[] value);

	String slowlogReset();

	Long slowlogLen();

	List<byte[]> slowlogGetBinary();

	List<byte[]> slowlogGetBinary(long entries);

	Long objectRefcount(byte[] key);

	byte[] objectEncoding(byte[] key);

	Long objectIdletime(byte[] key);

}
