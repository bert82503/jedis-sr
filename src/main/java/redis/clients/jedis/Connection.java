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

	/** 套接字是否出现异常 */
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
	public Connection(String host) {
		this.host = host;
	}

	/**
	 * 创建一条新的Redis链接。
	 * 
	 * @param host
	 * @param port
	 */
	public Connection(String host, int port) {
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
	 * 分2个步骤：
	 * 	1. 若当前的链接套接字还打开着，则直接返回；
	 * 	2. 否则，创建一条新的套接字链接。
	 * </pre>
	 */
	public void connect() {
		if (!this.isConnected()) {
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
				// TODO 连接超时与读取超时，使用同一个参数值不合理！
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
	protected Connection sendCommand(Command cmd, String... args) {
		final byte[][] bargs = new byte[args.length][];
		for (int i = 0; i < args.length; i++) {
			bargs[i] = SafeEncoder.encode(args[i]);
		}
		return this.sendCommand(cmd, bargs);
	}

	/**
	 * 发送一条命令到Redis服务器端。
	 * 
	 * <pre>
	 * 分3个步骤：
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
	protected Connection sendCommand(Command cmd, byte[]... args) {
		try {
			this.connect();
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

	protected Connection sendCommand(Command cmd) {
		return this.sendCommand(cmd, EMPTY_ARGS);
	}

	/**
	 * 关闭到Redis服务器的连接。
	 */
	@Override
	public void close() {
		this.disconnect();
	}

	/**
	 * 断开到Redis服务器的连接。
	 */
	public void disconnect() {
		if (this.isConnected()) {
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

	/**
	 * 链接是否出现异常了。
	 */
	public boolean isBroken() {
		return broken;
	}

	public void resetPipelinedCount() {
		pipelinedCommands = 0;
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
		this.flush();
		pipelinedCommands--;
		final byte[] resp = (byte[]) this.readProtocolWithCheckingBroken();
		if (null != resp) {
			return SafeEncoder.encode(resp);
		} else {
			return null;
		}
	}

	/**
	 * 获取一条命令的"字符串"执行结果。
	 */
	public String getBulkReply() {
		final byte[] result = this.getBinaryBulkReply();
		if (null != result) {
			return SafeEncoder.encode(result);
		} else {
			return null;
		}
	}

	/**
	 * 获取一条命令的"字节数组(二进制)"执行结果。
	 */
	public byte[] getBinaryBulkReply() {
		this.flush();
		pipelinedCommands--;
		return (byte[]) this.readProtocolWithCheckingBroken();
	}

	/**
	 * 获取一条命令的"长整型"执行结果。
	 */
	public Long getIntegerReply() {
		this.flush();
		pipelinedCommands--;
		return (Long) this.readProtocolWithCheckingBroken();
	}

	/**
	 * 获取一条批量命令的"字符串列表"执行结果。
	 */
	public List<String> getMultiBulkReply() {
		return BuilderFactory.STRING_LIST.build(this.getBinaryMultiBulkReply());
	}

	/**
	 * 获取一条批量命令的"字节数组列表"执行结果。
	 */
	@SuppressWarnings("unchecked")
	public List<byte[]> getBinaryMultiBulkReply() {
		this.flush();
		pipelinedCommands--;
		return (List<byte[]>) this.readProtocolWithCheckingBroken();
	}

	/**
	 * 获取一条批量命令的"原始对象列表"执行结果。
	 */
	@SuppressWarnings("unchecked")
	public List<Object> getRawObjectMultiBulkReply() {
		return (List<Object>) this.readProtocolWithCheckingBroken();
	}

	/**
	 * 获取一条批量命令的"对象列表"执行结果。
	 */
	public List<Object> getObjectMultiBulkReply() {
		this.flush();
		pipelinedCommands--;
		return this.getRawObjectMultiBulkReply();
	}

	/**
	 * 获取一条批量命令的"长整型列表"执行结果。
	 */
	@SuppressWarnings("unchecked")
	public List<Long> getIntegerMultiBulkReply() {
		this.flush();
		pipelinedCommands--;
		return (List<Long>) this.readProtocolWithCheckingBroken();
	}

	/**
	 * 获取所有命令的"对象列表"执行结果。
	 */
	public List<Object> getAll() {
		return this.getAll(0);
	}

	/**
	 * 获取若干条命令的"对象列表"执行结果。
	 */
	public List<Object> getAll(int except) {
		List<Object> all = new ArrayList<Object>();
		this.flush();
		while (pipelinedCommands > except) {
			try {
				all.add(this.readProtocolWithCheckingBroken());
			} catch (JedisDataException e) {
				// Bug 为什么把异常信息对象加载到返回结果中，而不是设置为null？？？
				all.add(e);
			}
			pipelinedCommands--;
		}
		return all;
	}

	/**
	 * 获取一条命令的"对象"执行结果。
	 */
	public Object getOne() {
		this.flush();
		pipelinedCommands--;
		return this.readProtocolWithCheckingBroken();
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * 设置链接永不断开。
	 */
	public void setTimeoutInfinite() {
		try {
			if (!this.isConnected()) {
				this.connect();
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

}
