package com.shivangi.telegram.redditsubscriber;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ResourceUtil {
	
	private ResourceUtil(){
	}

	public static ScheduledExecutorService executor;

	static {
		executor = Executors.newScheduledThreadPool(10);
	}

}
