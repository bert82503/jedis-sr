package redis.clients.jedis.exceptions;

/**
 * Redis数据异常。
 * 
 * @author huagang.li 2014年12月3日 下午1:41:51
 */
public class JedisDataException extends JedisException {

	private static final long serialVersionUID = 3878126572474819403L;

	public JedisDataException(String message) {
		super(message);
	}

	public JedisDataException(Throwable cause) {
		super(cause);
	}

	public JedisDataException(String message, Throwable cause) {
		super(message, cause);
	}

}
