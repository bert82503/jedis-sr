package redis.clients.util;

/**
 * "节点分片信息"抽象表示。
 * 
 * @author huagang.li 2014年12月2日 下午7:37:10
 */
public abstract class ShardInfo<R> {

	/** 分片权重 */
	private int weight;

	/**
	 * 创建一个使用默认分片权重({@link Sharded#DEFAULT_WEIGHT})的"分片信息"实例。
	 */
	public ShardInfo() {
		this.weight = Sharded.DEFAULT_WEIGHT;
	}

	/**
	 * 创建一个使用给定分片权重的"分片信息"实例。
	 * 
	 * @param weight
	 */
	public ShardInfo(int weight) {
		this.weight = weight;
	}

	/**
	 * 创建一条新的连接资源。
	 * 
	 * @return
	 */
	protected abstract R createResource();

	/**
	 * 返回节点的分片名称。
	 * 
	 * @return
	 */
	public abstract String getName();

	/**
	 * 返回该节点的分片权重。
	 * 
	 * @return
	 */
	public int getWeight() {
		return this.weight;
	}

}
