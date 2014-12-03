package redis.clients.util;

import java.io.UnsupportedEncodingException;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;

/**
 * 内容安全编码器，目的是兼容JDK 1.5。
 * <p>
 * The only reason to have this is to be able to compatible with java 1.5 :(
 */
public class SafeEncoder {

	/**
	 * 将给定的多个字符串转换成字节数组列表。
	 * 
	 * @param strs
	 * @return
	 */
	public static byte[][] encodeMany(final String... strs) {
		byte[][] many = new byte[strs.length][];
		for (int i = 0; i < strs.length; i++) {
			many[i] = encode(strs[i]);
		}
		return many;
	}

	/**
	 * 将给定的字符串转换成字节数组。
	 * 
	 * @param str
	 *            待转换的字符串
	 * @return
	 */
	public static byte[] encode(final String str) {
		try {
			if (str == null) {
				throw new JedisDataException(
						"value sent to redis cannot be null");
			}
			return str.getBytes(Protocol.CHARSET);
		} catch (UnsupportedEncodingException e) {
			throw new JedisException(e);
		}
	}

	/**
	 * 将给定的字节数组转换成字符串。
	 * 
	 * @param data
	 *            待转换的字节数组
	 * @return
	 */
	public static String encode(final byte[] data) {
		try {
			return new String(data, Protocol.CHARSET);
		} catch (UnsupportedEncodingException e) {
			throw new JedisException(e);
		}
	}

}
