package br.ufmg.dcc.vod.spiderpig.master.walker.tracker;

import com.google.common.hash.BloomFilter;

/**
 * A tracker which make's use of a bloom filter. In this tracker,
 * the {@code wasCrawled} method can return some false positives.
 * 
 * @author Flavio Figueiredo - flaviovdf 'at' gmail.com
 * @param <T> Type of object to store
 * @see {@link com.google.common.hash.BloomFilter} 
 */
public class BloomFilterTracker<T> implements Tracker<T> {

	private final BloomFilter<T> bloomFilter;
	private int size;

	/**
	 * Creates an empty tracker backed by a bloom filter. 
	 */
	protected BloomFilterTracker(BloomFilter<T> bloomFilter) {
		this.bloomFilter = bloomFilter;
		this.size = 0;
	}

	@Override
	public boolean addCrawled(T t) {
		boolean returnVal = this.bloomFilter.put(t);
		if (returnVal) {
			this.size++;
		}
		return returnVal;
	}

	@Override
	public boolean wasCrawled(T t) {
		return this.bloomFilter.mightContain(t);
	}

	@Override
	public int numCrawled() {
		return this.size;
	}

}
