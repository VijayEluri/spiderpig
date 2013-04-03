package br.ufmg.dcc.vod.spiderpig.master.walker;

import java.util.List;

import br.ufmg.dcc.vod.spiderpig.protocol_buffers.Ids.CrawlID;

/**
 * A walk is responsible for defining the network walk strategy for
 * crawlers. Basically, implementors of this class will return a list
 * of ids to be queued for crawling based on the result of a previous crawled
 * id. 
 * 
 * @author Flavio Figueiredo - flaviovdf 'at' gmail.com
 */
public interface Walker {

	/**
	 * Returns a list of id's to crawl based on the current result.
	 * 
	 * @param crawled ID crawled.
	 * @param links Links discovered
	 * 
	 * @return A new set of ids to crawl
	 */
	public List<CrawlID> getToWalk(CrawlID crawled, List<CrawlID> links);
	
	/**
	 * Add seed ID the walker. Seeds are initial ids which may require special
	 * treatment by different walkers.
	 * 
	 * @param seed seed ID.
	 */
	public void addSeedID(CrawlID seed);

	/**
	 * After seeds are indicated, this method will return which ids
	 * should be crawled.
	 * 
	 * @return List of ids to crawl
	 */
	public List<CrawlID> getSeedDispatch();
	
}
