/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package redis.clients.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * 非同步的Redis缓冲过滤输入流。
 * 
 * @author huagang.li 2014年12月3日 上午11:00:16
 */
public class RedisInputStream extends FilterInputStream {

	/** 文件末尾标记(End of File) */
	private static final int EOF = -1;

	/** 缓冲区 */
	protected final byte buf[];

	/** 内容读取位置 */
	protected int count;

	/** 缓冲区内容的实际有效长度 */
	protected int limit;

	/**
	 * 创建一个给定缓冲区大小的Redis过滤输入流。
	 * 
	 * @param in
	 *            输入流
	 * @param bufferSize
	 *            缓冲区大小
	 */
	public RedisInputStream(InputStream in, int bufferSize) {
		super(in);
		if (bufferSize <= 0) {
			throw new IllegalArgumentException("Buffer size <= 0");
		}
		buf = new byte[bufferSize];
	}

	/**
	 * 创建一个具有8KB大小缓冲区的Redis过滤输入流。
	 * 
	 * @param in
	 *            输入流
	 */
	public RedisInputStream(InputStream in) {
		this(in, 8192); // 8KB
	}

	/**
	 * 读取一个字节内容。
	 * 
	 * @return
	 * @throws IOException
	 */
	public byte readByte() throws IOException {
		if (count == limit) { // 缓冲区的内容已被读完，得重新加载新的数据
			fill();
		}

		return buf[count++];
	}

	/**
	 * 读取一行内容。
	 * 
	 * @return 一行数据，或抛出"Jedis连接运行时异常"定义
	 */
	public String readLine() {
		int b;
		byte c;
		StringBuilder sb = new StringBuilder(); // 保存一行内容

		try {
			while (true) {
				if (count == limit) { // 缓冲区的内容已被读完，得重新加载新的数据
					fill();
				}
				if (limit == EOF) { // 没有数据了
					break;
				}

				b = buf[count++];
				if (b == '\r') {
					if (count == limit) { // 缓冲区的内容正好被读完，得重新加载新的数据
						fill();
					}

					if (limit == EOF) { // 没有数据了
						sb.append((char) b);
						break;
					}
					// 继续处理
					c = buf[count++];
					if (c == '\n') { // 忽略换行符（'\r\n'）
						break;
					}
					sb.append((char) b);
					sb.append((char) c);
				} else {
					sb.append((char) b);
				}
			}
		} catch (IOException e) {
			// 读取一行内容出现异常
			throw new JedisConnectionException(e);
		}
		String reply = sb.toString();
		if (reply.isEmpty()) {
			// 服务端已关闭该连接
			throw new JedisConnectionException(
					"It seems like server has closed the connection.");
		}
		return reply;
	}

	/**
	 * 从输入流读取给定长度的字节数组数据。
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (count == limit) { // 缓冲区的内容已被读完，得重新加载新的数据
			fill();
			if (limit == EOF) {
				return -1;
			}
		}
		final int length = Math.min(limit - count, len);
		System.arraycopy(buf, count, b, off, length);
		count += length;
		return length;
	}

	/*
	 * 读取输入流内容，并保存到缓冲区里。
	 */
	private void fill() throws IOException {
		this.limit = in.read(buf);
		this.count = 0;
	}

}
