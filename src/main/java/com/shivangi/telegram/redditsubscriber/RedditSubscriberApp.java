package com.shivangi.telegram.redditsubscriber;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


public class RedditSubscriberApp {

	public static void main(String[] args) {

		ApiContextInitializer.init();

		TelegramBotsApi botsApi = new TelegramBotsApi();

		try {
			botsApi.registerBot(new RedditSubscriberBot());
			System.out.println("RedditSubscriberBot is started!");
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
	}
}
