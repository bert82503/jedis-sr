package redis.clients.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * The class implements a buffered output stream without synchronization. There
 * are also special operations like in-place string encoding. This stream fully
 * ignore mark/reset and should not be used outside Jedis.
 * <p>
 * 非同步的Redis缓冲过滤输出流，包括一些特殊操作：
 * 占位字符串编码、完全忽略"标记/重置"（不应在Jedis之外使用）。
 */
public final class RedisOutputStream extends FilterOutputStream {

	/** 缓冲区 */
	protected final byte buf[];

	/** 缓冲区内容的实际长度 */
	protected int count;

	public RedisOutputStream(final OutputStream out) {
		this(out, 8192); // 缓冲区默认大小为 8KB
	}

	public RedisOutputStream(final OutputStream out, final int size) {
		super(out);
		if (size <= 0) {
			throw new IllegalArgumentException("Buffer size <= 0");
		}
		buf = new byte[size];
	}

	/*
	 * 刷新缓冲区。
	 */
	private void flushBuffer() throws IOException {
		if (count > 0) {
			out.write(buf, 0, count);
			count = 0;
		}
	}

    /**
     * 写入一个字节内容到这个输出流。
     *
     * @param b
     * @throws IOException
     */
	public void write(final byte b) throws IOException {
		if (count == buf.length) { // 缓冲区满了
			flushBuffer();
		}
		buf[count++] = b;
	}

	@Override
	public void write(final byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public void write(final byte[] b, final int off, final int len)
			throws IOException {
		if (len >= buf.length) { // 要写的内容太多，先刷新缓冲区，然后将字节数组内容直接写入到输出流
			flushBuffer();
			out.write(b, off, len);
		} else {
			if (len >= buf.length - count) {
				flushBuffer();
			}
			// 缓冲区内容可以容纳下待写的字节数组内容
			System.arraycopy(b, off, buf, count, len);
			count += len;
		}
	}

	/**
	 * 写入文本数据到这个输出流。
	 *
	 * @param str 文本数据
	 * @throws IOException
	 */
	public void writeAsciiCrLf(final String str) throws IOException {
		int size = str.length();
		for (int i = 0; i < size; ++i) {
			if (count == buf.length) {
				flushBuffer();
			}
			buf[count++] = (byte) str.charAt(i);
		}

		writeCrLf();
	}

	/**
	 * 判断这个字符是否是代理字符。
	 *
	 * @param ch
	 * @return
	 */
	public static boolean isSurrogate(final char ch) {
		return ch >= Character.MIN_SURROGATE && ch <= Character.MAX_SURROGATE;
	}

    /**
     * 计算字符串对应的"UTF-8"编码后的长度。
     *
     * @param str
     * @return
     */
	public static int utf8Length(final String str) {
		int utfLen = 0;

		int strLen = str.length();
		for (int i = 0; i < strLen; ++i) {
			char c = str.charAt(i);
			if (c < 0x80) {
				utfLen++;
			} else if (c < 0x800) {
				utfLen += 2;
			} else if (isSurrogate(c)) {
				i++;
				utfLen += 4;
			} else {
				utfLen += 3;
			}
		}

		return utfLen;
	}

    /**
     * 写入"换行符(\r\n)"。
     *
     * @throws IOException
     */
	public void writeCrLf() throws IOException {
		if (2 >= buf.length - count) {
			flushBuffer();
		}

		buf[count++] = '\r';
		buf[count++] = '\n';
	}

    /**
     * 写入经过"UTF-8"编码后的文本数据到这个输出流。
     *
     * @param str
     * @throws IOException
     */
	public void writeUtf8CrLf(final String str) throws IOException {
		int strLen = str.length();

		int i;
		for (i = 0; i < strLen; i++) {
			char c = str.charAt(i);
			if (!(c < 0x80))
				break;
			if (count == buf.length) {
				flushBuffer();
			}
			buf[count++] = (byte) c; // 字节值范围
		}

		for (; i < strLen; i++) {
			char c = str.charAt(i);
			if (c < 0x80) {
				if (count == buf.length) {
					flushBuffer();
				}
				buf[count++] = (byte) c; // 字节值范围
			} else if (c < 0x800) {
				if (2 >= buf.length - count) {
					flushBuffer();
				}
				// 2字节编码
				buf[count++] = (byte) (0xc0 | (c >> 6));
				buf[count++] = (byte) (0x80 | (c & 0x3f));
			} else if (isSurrogate(c)) {
				if (4 >= buf.length - count) {
					flushBuffer();
				}
				// 4字节编码
				int uc = Character.toCodePoint(c, str.charAt(i++));
				buf[count++] = ((byte) (0xf0 | ((uc >> 18))));
				buf[count++] = ((byte) (0x80 | ((uc >> 12) & 0x3f)));
				buf[count++] = ((byte) (0x80 | ((uc >> 6) & 0x3f)));
				buf[count++] = ((byte) (0x80 | (uc & 0x3f)));
			} else {
				if (3 >= buf.length - count) {
					flushBuffer();
				}
				// 3字节编码
				buf[count++] = ((byte) (0xe0 | ((c >> 12))));
				buf[count++] = ((byte) (0x80 | ((c >> 6) & 0x3f)));
				buf[count++] = ((byte) (0x80 | (c & 0x3f)));
			}
		}

		writeCrLf();
	}

    private final static int[] sizeTable = { 9, 99, 999, 9999, 99999, 999999,
	    9999999, 99999999, 999999999, Integer.MAX_VALUE };

    private final static byte[] DigitTens = { '0', '0', '0', '0', '0', '0',
	    '0', '0', '0', '0', '1', '1', '1', '1', '1', '1', '1', '1', '1',
	    '1', '2', '2', '2', '2', '2', '2', '2', '2', '2', '2', '3', '3',
	    '3', '3', '3', '3', '3', '3', '3', '3', '4', '4', '4', '4', '4',
	    '4', '4', '4', '4', '4', '5', '5', '5', '5', '5', '5', '5', '5',
	    '5', '5', '6', '6', '6', '6', '6', '6', '6', '6', '6', '6', '7',
	    '7', '7', '7', '7', '7', '7', '7', '7', '7', '8', '8', '8', '8',
	    '8', '8', '8', '8', '8', '8', '9', '9', '9', '9', '9', '9', '9',
	    '9', '9', '9', };

    private final static byte[] DigitOnes = { '0', '1', '2', '3', '4', '5',
	    '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8',
	    '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1',
	    '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4',
	    '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7',
	    '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
	    '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3',
	    '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6',
	    '7', '8', '9', };

    /** 数值字符 */
    private final static byte[] digits = { '0', '1', '2', '3', '4', '5', '6',
	    '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
	    'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w',
	    'x', 'y', 'z' };

	/**
	 * 写入整型数据到这个输出流。
	 *
	 * @param value
	 * @throws IOException
	 */
	public void writeIntCrLf(int value) throws IOException {
		// 负数转换为正数
		if (value < 0) {
			write((byte) '-');
			value = -value;
		}

		int size = 0;
		while (value > sizeTable[size]) {
			size++;
		}

		size++;
		if (size >= buf.length - count) {
			flushBuffer();
		}

		int q, r;
		int charPos = count + size;

		while (value >= 65536) {
			q = value / 100;
			r = value - ((q << 6) + (q << 5) + (q << 2));
			value = q;
			buf[--charPos] = DigitOnes[r];
			buf[--charPos] = DigitTens[r];
		}

		for (;;) {
			q = (value * 52429) >>> (16 + 3);
			r = value - ((q << 3) + (q << 1));
			buf[--charPos] = digits[r];
			value = q;
			if (value == 0)
				break;
		}
		count += size;

		writeCrLf();
	}

	@Override
	public void flush() throws IOException {
		flushBuffer();
		out.flush();
	}

}
