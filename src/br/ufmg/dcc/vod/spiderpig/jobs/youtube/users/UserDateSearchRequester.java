package br.ufmg.dcc.vod.spiderpig.jobs.youtube.users;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import br.ufmg.dcc.vod.spiderpig.common.Tuple;
import br.ufmg.dcc.vod.spiderpig.jobs.QuotaException;
import br.ufmg.dcc.vod.spiderpig.jobs.Requester;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

public class UserDateSearchRequester 
		implements Requester<Tuple<List<String>, byte[]>> {

	private static final int _403_QUOTA_ERR = 403;
	
	private final YouTube youtube;
	private final String apiKey;

	public UserDateSearchRequester(YouTube youtube, String apiKey) {
		this.youtube = youtube;
		this.apiKey = apiKey;
	}

	@Override
	public Tuple<List<String>, byte[]> performRequest(String dates) 
			throws IOException {
		String[] split = dates.split("\\s");
		
		String afterDate = split[0];
		String beforeDate = split[1];
		
		try {
			YouTube.Search.List search = youtube.search().list("id");
			search.setKey(this.apiKey);
			search.setPublishedAfter(DateTime.parseRfc3339(afterDate));
			search.setPublishedBefore(DateTime.parseRfc3339(beforeDate));
			search.setType("channel");
			search.setSafeSearch("none");
			search.setOrder("date");
			search.setMaxResults(50l);
			
			SearchListResponse response = search.execute();
		
			StringBuilder channelIdsBuffer = new StringBuilder();
			List<String> channelIds = new ArrayList<>();
			
			String nextPageToken;
			int numResults = 0;
			do {
				List<SearchResult> items = response.getItems();
				for (SearchResult searchResult : items) {
					ResourceId rId = searchResult.getId();
					if (rId != null) {
						String channelId = (String) rId.get("channelId");
						
						channelIds.add(channelId);
						channelIdsBuffer.append(channelId);
						channelIdsBuffer.append(System.lineSeparator());
						++numResults;
					}
				}
				
				nextPageToken = response.getNextPageToken();
				if (nextPageToken != null) {
					search.setPageToken(nextPageToken);
					response = search.execute();
				}
			} while (nextPageToken != null);
			
			channelIdsBuffer.append(numResults);
			return new Tuple<>(channelIds, 
					channelIdsBuffer.toString().getBytes());
			
		} catch (GoogleJsonResponseException e) {
			GoogleJsonError details = e.getDetails();
			
			if (details != null) {
				Object statusCode = details.get("code");
				
				if (statusCode.equals(_403_QUOTA_ERR)) {
					throw new QuotaException(e);
				} else {
					throw e;
				}
			} else {
				throw e;
			}
		}
	}
}