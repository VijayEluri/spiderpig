package br.ufmg.dcc.vod.spiderpig.jobs.youtube.users.subs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonError.ErrorInfo;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.Subscription;
import com.google.api.services.youtube.model.SubscriptionListResponse;
import com.google.common.collect.Sets;

import br.ufmg.dcc.vod.spiderpig.common.config.ConfigurableBuilder;
import br.ufmg.dcc.vod.spiderpig.jobs.ConfigurableRequester;
import br.ufmg.dcc.vod.spiderpig.jobs.CrawlResultFactory;
import br.ufmg.dcc.vod.spiderpig.jobs.PayloadsFactory;
import br.ufmg.dcc.vod.spiderpig.jobs.QuotaException;
import br.ufmg.dcc.vod.spiderpig.jobs.Request;
import br.ufmg.dcc.vod.spiderpig.jobs.youtube.UnableToCrawlException;
import br.ufmg.dcc.vod.spiderpig.jobs.youtube.YTConstants;
import br.ufmg.dcc.vod.spiderpig.protocol_buffers.Ids.CrawlID;
import br.ufmg.dcc.vod.spiderpig.protocol_buffers.Worker.CrawlResult;

public class UserDataRequester implements ConfigurableRequester {

    private static final String SUB_DETAILS = "id,snippet,contentDetails";
    private static final String USER_DETAILS = "id,snippet,brandingSettings," +
            "contentDetails,invideoPromotion,statistics,topicDetails";
    private final Set<String> SUB_ORDERS = 
            Sets.newHashSet("relevance", "unread", "alphabetical");
    
    private static final int _403_QUOTA_ERR = 403;
    
    private YouTube youtube;
    private String apiKey;
    
    public CrawlResult performRequest(CrawlID crawlID) throws QuotaException {
        CrawlResultFactory crawlResultBuilder = new CrawlResultFactory(crawlID);
        CrawlResult result = null;
        
        String userId = crawlID.getId();
        Channel userChannel = null;
        PayloadsFactory payloadBuilder = null;
        
        try {
            userChannel = getUserChannel(userId);
            payloadBuilder = new PayloadsFactory();
            payloadBuilder.addPayload("userId-data-" + userId, 
                    userChannel.toPrettyString().getBytes());
        } catch (QuotaException e) {
            throw e;
        } catch (IOException e) {
            result = crawlResultBuilder.buildNonQuotaError(
                    new UnableToCrawlException(e));
        }
        
        List<CrawlID> toFollow = new ArrayList<>();
        if (result == null) { //No error for basic user data. Get links
            for (String order : SUB_ORDERS) {
                try {
                    List<Subscription> subs = getLinks(userId, order);
                    for (Subscription sub : subs) {
                        String nextChannel =
                                sub.getSnippet().getResourceId().getChannelId();
                        String subId = sub.getId();
                        payloadBuilder.addPayload(
                                "userId-" + userId + "-other-" + nextChannel + 
                                "-sub-" + subId,
                                sub.toPrettyString().getBytes());
                        
                        CrawlID crawlIDtoFollow = CrawlID.newBuilder().
                                setId(nextChannel).
                                build();
                        toFollow.add(crawlIDtoFollow);
                    }
                } catch (QuotaException e) {
                    throw e;
                } catch (IOException e) {
                    // IGNORE we have user data
                }
            }
            result = crawlResultBuilder.buildOK(payloadBuilder.build(), 
                    toFollow);
        }
        
        return result;
    }

    private List<Subscription> getLinks(String userID, String order) 
            throws IOException {
        YouTube.Subscriptions.List subsList = 
                youtube.subscriptions().list(SUB_DETAILS);
        
        subsList.setKey(this.apiKey);
        subsList.setMaxResults(50l);
        subsList.setChannelId(userID);
        subsList.setOrder(order);
        
        List<Subscription> returnValue = new ArrayList<>();
        String nextPageToken;
        try {
            SubscriptionListResponse response = subsList.execute();
            do {
                List<Subscription> items = response.getItems();
                returnValue.addAll(items);
                
                nextPageToken = response.getNextPageToken();
                if (nextPageToken != null) {
                    subsList.setPageToken(nextPageToken);
                    response = subsList.execute();
                }
            } while (nextPageToken != null);
            
        } catch (GoogleJsonResponseException e) {
            List<ErrorInfo> errors = e.getDetails().getErrors();
            if (_403_QUOTA_ERR == e.getStatusCode()) {
                for (ErrorInfo ei : errors) {
                    if (ei.getReason().equals("dailyLimitExceeded")) {
                        throw new QuotaException(e);
                    } else if (ei.getReason().equals("subscriptionForbidden")) {
                        return Collections.emptyList();
                    }
                }
            }
            throw e;
        }
        
        return returnValue;
    }

    private Channel getUserChannel(String userID)
            throws IOException, QuotaException, GoogleJsonResponseException {
        
        YouTube.Channels.List userList = youtube.channels().list(USER_DETAILS);
        userList.setKey(this.apiKey);
        userList.setMaxResults(50l);
        userList.setId(userID);
        
        try {
            ChannelListResponse response = userList.execute();
            List<Channel> items = response.getItems();
            if (items.isEmpty()) {
                throw new IOException("User not found " + userID);
            } else {
                return items.get(0);
            }
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


    @Override
    public Set<String> getRequiredParameters() {
        return Sets.newHashSet(YTConstants.API_KEY);
    }


    @Override
    public void configurate(Configuration configuration, 
            ConfigurableBuilder configurableBuilder) {
        this.apiKey = configuration.getString(YTConstants.API_KEY);
        this.youtube = YTConstants.buildYoutubeService();
    }
    
	@Override
	public Request createRequest(final CrawlID crawlID) {
		return new Request() {
			@Override
			public CrawlResult continueRequest() throws QuotaException {
				return performRequest(crawlID);
			}
		};
	}
}