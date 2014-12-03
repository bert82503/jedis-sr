package redis.clients.jedis;

import java.util.List;

/**
 * 处理二进制数据格式的"Redis脚本命令"定义。
 * 
 * @author huagang.li 2014年12月3日 下午5:11:58
 */
public interface BinaryScriptingCommands {

	Object eval(byte[] script, byte[] keyCount, byte[]... params);

	Object eval(byte[] script, int keyCount, byte[]... params);

	Object eval(byte[] script, List<byte[]> keys, List<byte[]> args);

	Object eval(byte[] script);

	Object evalsha(byte[] script);

	Object evalsha(byte[] sha1, List<byte[]> keys, List<byte[]> args);

	Object evalsha(byte[] sha1, int keyCount, byte[]... params);

	// TODO: should be Boolean, add singular version
	List<Long> scriptExists(byte[]... sha1);

	byte[] scriptLoad(byte[] script);

	String scriptFlush();

	String scriptKill();

}
