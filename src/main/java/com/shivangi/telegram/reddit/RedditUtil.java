package com.shivangi.telegram.reddit;

import java.util.ArrayList;
import java.util.List;

import com.github.jreddit.entity.Submission;
import com.github.jreddit.retrieval.Submissions;
import com.github.jreddit.retrieval.params.SubmissionSort;
import com.github.jreddit.utils.restclient.HttpRestClient;
import com.github.jreddit.utils.restclient.RestClient;

public class RedditUtil {

	private static Submissions submissions;
	
	private RedditUtil(){
	}

	private static void initialize() {

		RestClient restClient = new HttpRestClient();
		submissions = new Submissions(restClient);

	}

	public static List<String> getTopPost(String subreddit, int maxResults) {
		initialize();
		List<String> posts = new ArrayList<>();
		// Retrieve submissions of a submission
		List<Submission> submissionsSubreddit = submissions.ofSubreddit(subreddit, SubmissionSort.TOP, -1, 10, null,
				null, true);

		int numResults = 1;
		for (Submission submission : submissionsSubreddit) {
			posts.add("https://www.reddit.com" + submission.getPermalink());
			if (numResults++ == maxResults) {
				break;
			}
		}
		return posts;
	}

}
