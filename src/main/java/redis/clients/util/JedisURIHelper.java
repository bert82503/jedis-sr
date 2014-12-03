package redis.clients.util;

import java.net.URI;

/**
 * Redis协议的URI帮助工具类。
 * 
 * @author huagang.li 2014年12月3日 下午5:29:44
 */
public class JedisURIHelper {

	/**
	 * 从URI中获取服务器密码信息。
	 * 
	 * @param uri
	 * @return
	 */
	public static String getPassword(URI uri) {
		String userInfo = uri.getUserInfo();
		if (userInfo != null) {
			return userInfo.split(":", 2)[1];
		}
		return null;
	}

	/**
	 * 从URI中获取Redis数据库索引。
	 * 
	 * @param uri
	 * @return
	 */
	public static Integer getDBIndex(URI uri) {
		String[] pathSplit = uri.getPath().split("/", 2);
		if (pathSplit.length > 1) {
			String dbIndexStr = pathSplit[1];
			if (dbIndexStr.isEmpty()) {
				return 0;
			}
			return Integer.parseInt(dbIndexStr);
		} else {
			return 0;
		}
	}

}
