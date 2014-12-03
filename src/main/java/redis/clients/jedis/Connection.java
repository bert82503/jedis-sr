package redis.clients.jedis;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.util.RedisInputStream;
import redis.clients.util.RedisOutputStream;
import redis.clients.util.SafeEncoder;

/**
 * "Redis链接"实现。
 * 
 * @author huagang.li 2014年12月3日 下午1:18:04
 */
public class Connection implements Closeable {

	/** 主机域名/IP */
	private String host;
	/** 端口号 */
	private int port = Protocol.DEFAULT_PORT;

	/** 链接套接字 */
	private Socket socket;
	/** 输出流 */
	private RedisOutputStream outputStream;
	/** 输入流 */
	private RedisInputStream inputStream;
	/** 套接字存活保持时间、连接超时时间、读取超时时间(ms) */
	private int timeout = Protocol.DEFAULT_TIMEOUT;

	/** 链接是否阻塞了 */
	private boolean broken = false;
	/** 已进入管道的命令计数器 */
	private int pipelinedCommands = 0;

	public Connection() {
	}

	/**
	 * 创建一条新的Redis链接。
	 * 
	 * @param host
	 */
	public Connection(final String host) {
		this.host = host;
	}

	/**
	 * 创建一条新的Redis链接。
	 * 
	 * @param host
	 * @param port
	 */
	public Connection(final String host, final int port) {
		this.host = host;
		this.port = port;
	}

	/**
	 * 检测套接字链接是否还连接着。
	 * 
	 * @return
	 */
	public boolean isConnected() {
		return socket != null && socket.isBound() && !socket.isClosed()
				&& socket.isConnected() && !socket.isInputShutdown()
				&& !socket.isOutputShutdown();
	}

	/**
	 * 连接到Redis服务器。
	 * 
	 * <pre>
	 * 分二步骤：
	 * 	1. 若当前的链接套接字还打开着，则直接返回；
	 * 	2. 否则，创建一条新的套接字链接。
	 * </pre>
	 */
	public void connect() {
		if (!isConnected()) {
			// 当前的链接套接字已被关闭，需要重新建立一条新的链接
			try {
				socket = new Socket();
				socket.setReuseAddress(true);
				// Will monitor the TCP connection is valid (使用长连接技术)
				socket.setKeepAlive(true);
				// Socket buffer Whether closed, to ensure timely delivery of
				// data (确保数据实时发送)
				socket.setTcpNoDelay(true);
				// Control calls close() method, the underlying socket is
				// closed immediately (即使背后的套接字被瞬间关闭，也确保close()被调用)
				socket.setSoLinger(true, 0);

				// 创建一条新的连接到后端Redis服务器的链接
				socket.connect(new InetSocketAddress(host, port), timeout);
				socket.setSoTimeout(timeout);

				outputStream = new RedisOutputStream(socket.getOutputStream());
				inputStream = new RedisInputStream(socket.getInputStream());
			} catch (IOException ex) {
				broken = true;
				throw new JedisConnectionException(ex);
			}
		}
	}

	/**
	 * 发送一条命令到Redis服务器端。
	 * 
	 * @param cmd
	 *            Redis命令
	 * @param args
	 *            参数列表
	 * @return
	 */
	protected Connection sendCommand(final Command cmd, final String... args) {
		final byte[][] bargs = new byte[args.length][];
		for (int i = 0; i < args.length; i++) {
			bargs[i] = SafeEncoder.encode(args[i]);
		}
		return sendCommand(cmd, bargs);
	}

	/**
	 * 发送一条命令到Redis服务器端。
	 * 
	 * <pre>
	 * 分三步骤：
	 * 	1. 连接到Redis服务器；
	 * 	2. 发送命令（基于{@link Protocol#sendCommand(RedisOutputStream, Command, byte[]...)}实现）；
	 * 	3. 进入管道的命令计数器加1。
	 * </pre>
	 * 
	 * @param cmd
	 *            Redis命令
	 * @param args
	 *            参数列表
	 * @return
	 */
	protected Connection sendCommand(final Command cmd, final byte[]... args) {
		try {
			connect();
			Protocol.sendCommand(outputStream, cmd, args);
			pipelinedCommands++;
			return this;
		} catch (JedisConnectionException ex) {
			// Any other exceptions related to connection?
			broken = true;
			throw ex;
		}
	}

	/** 空参数列表 */
	private static final byte[][] EMPTY_ARGS = new byte[0][];

	protected Connection sendCommand(final Command cmd) {
		return sendCommand(cmd, EMPTY_ARGS);
	}

	@Override
	public void close() {
		disconnect();
	}

	/**
	 * 断开到Redis服务器的连接。
	 */
	public void disconnect() {
		if (isConnected()) {
			// 按顺序依次关闭 输入流、输出流、套接字（释放资源）
			try {
				inputStream.close();
				outputStream.close();
				if (!socket.isClosed()) {
					socket.close();
				}
			} catch (IOException ex) {
				broken = true;
				throw new JedisConnectionException(ex);
			}
		}
	}

	/**
	 * 返回链接套接字。
	 */
	public Socket getSocket() {
		return socket;
	}

	public String getHost() {
		return host;
	}

	public void setHost(final String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(final int port) {
		this.port = port;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(final int timeout) {
		this.timeout = timeout;
	}

	/**
	 * 设置链接永不断开。
	 */
	public void setTimeoutInfinite() {
		try {
			if (!isConnected()) {
				connect();
			}
			socket.setKeepAlive(true);
			socket.setSoTimeout(0); // 0：表示链接永不超时
		} catch (SocketException ex) {
			broken = true;
			throw new JedisConnectionException(ex);
		}
	}

	public void rollbackTimeout() {
		try {
			socket.setSoTimeout(timeout);
			socket.setKeepAlive(false);
		} catch (SocketException ex) {
			broken = true;
			throw new JedisConnectionException(ex);
		}
	}

	/**
	 * 刷新输出流。
	 */
	protected void flush() {
		try {
			outputStream.flush();
		} catch (IOException ex) {
			broken = true;
			throw new JedisConnectionException(ex);
		}
	}

	/**
	 * 读取命令执行的响应信息。
	 * <p>
	 * 基于{@link Protocol#read(RedisInputStream)}实现
	 */
	protected Object readProtocolWithCheckingBroken() {
		try {
			return Protocol.read(inputStream);
		} catch (JedisConnectionException exc) {
			broken = true;
			throw exc;
		}
	}

	/**
	 * 获取请求的响应状态码。
	 */
	protected String getStatusCodeReply() {
		flush();
		pipelinedCommands--;
		final byte[] resp = (byte[]) readProtocolWithCheckingBroken();
		if (null == resp) {
			return null;
		} else {
			return SafeEncoder.encode(resp);
		}
	}

	public String getBulkReply() {
		final byte[] result = getBinaryBulkReply();
		if (null != result) {
			return SafeEncoder.encode(result);
		} else {
			return null;
		}
	}

	public byte[] getBinaryBulkReply() {
		flush();
		pipelinedCommands--;
		return (byte[]) readProtocolWithCheckingBroken();
	}

	public Long getIntegerReply() {
		flush();
		pipelinedCommands--;
		return (Long) readProtocolWithCheckingBroken();
	}

	public List<String> getMultiBulkReply() {
		return BuilderFactory.STRING_LIST.build(getBinaryMultiBulkReply());
	}

	@SuppressWarnings("unchecked")
	public List<byte[]> getBinaryMultiBulkReply() {
		flush();
		pipelinedCommands--;
		return (List<byte[]>) readProtocolWithCheckingBroken();
	}

	public void resetPipelinedCount() {
		pipelinedCommands = 0;
	}

	@SuppressWarnings("unchecked")
	public List<Object> getRawObjectMultiBulkReply() {
		return (List<Object>) readProtocolWithCheckingBroken();
	}

	public List<Object> getObjectMultiBulkReply() {
		flush();
		pipelinedCommands--;
		return getRawObjectMultiBulkReply();
	}

	@SuppressWarnings("unchecked")
	public List<Long> getIntegerMultiBulkReply() {
		flush();
		pipelinedCommands--;
		return (List<Long>) readProtocolWithCheckingBroken();
	}

	public List<Object> getAll() {
		return getAll(0);
	}

	public List<Object> getAll(int except) {
		List<Object> all = new ArrayList<Object>();
		flush();
		while (pipelinedCommands > except) {
			try {
				all.add(readProtocolWithCheckingBroken());
			} catch (JedisDataException e) {
				all.add(e);
			}
			pipelinedCommands--;
		}
		return all;
	}

	public Object getOne() {
		flush();
		pipelinedCommands--;
		return readProtocolWithCheckingBroken();
	}

	public boolean isBroken() {
		return broken;
	}

}
