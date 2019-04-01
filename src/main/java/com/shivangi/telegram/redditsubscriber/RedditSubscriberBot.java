package com.shivangi.telegram.redditsubscriber;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.shivangi.telegram.reddit.RedditUtil;

public class RedditSubscriberBot extends TelegramLongPollingBot {

	private static Map<String, Map<String, ScheduledFuture<?>>> userSubscriptions = new HashMap<>();

	// Get from @BotFather
	@Override
	public String getBotUsername() {
		return "";
	}

	// Get from @BotFather
	@Override
	public String getBotToken() {
		return "";
	}

	@Override
	public void onUpdateReceived(Update update) {

		if (update.getMessage() != null) {

			String chatUserName = update.getMessage().getChat().getUserName();

			if (update.hasMessage() && update.getMessage().hasText())

			{
				String txt = update.getMessage().getText().toLowerCase().trim();
				Long chatId = update.getMessage().getChatId();

				if (txt.startsWith("/start") || txt.equals("commands")) {
					initPost(chatId);

				} else if (txt.startsWith("status")) {
					postMsg(chatId, "I am live 🤖");
				}

				else if (txt.equals("list")) {
					listSubscrption(chatUserName, chatId);
				}

				else if (txt.startsWith("subscribe")) {
					boolean isValid = validateSubscribeCommand(chatId, txt);
					if (isValid) {
						String[] parts = txt.split(" ");
						String subreddit = parts[1].trim();
						// frequency is in hours
						int frequency = Integer.parseInt(parts[2].trim());
						subscribe(chatUserName, chatId, subreddit, frequency);
					}
				}

				else if (txt.startsWith("unsubscribe") && !txt.equals("unsubscribeall")) {
					boolean isValid = validateUnsubscribeCommand(chatId, txt);
					if (isValid) {
						String[] parts = txt.split(" ");
						String subreddit = parts[1].trim();
						unsubscribe(chatUserName, chatId, subreddit);
					}
				}

				else if (txt.equals("unsubscribeall")) {
					unsubscribeAll(chatUserName, chatId);
				}

				else {
					postMsg(chatId, "Invalid command. Write `commands` to see all supported commands.");
				}
			}
		}
	}

	private boolean isUserSubscriptionExist(String user, String subreddit) {
		if (userSubscriptions.get(user) == null) {
			return false;
		} else {
			Map<String, ScheduledFuture<?>> subscriptions = userSubscriptions.get(user);
			return subscriptions.get(subreddit) != null;
		}

	}

	private void unsubscribe(String chatUserName, Long chatId, String subreddit) {
		if (!isUserSubscriptionExist(chatUserName, subreddit)) {
			postMsg(chatId, "Not subscribed to " + subreddit);
		} else {
			removeUserSubcription(chatUserName, subreddit);
			postMsg(chatId, "Successfully unsubscribed from " + subreddit + " 🙂");
		}
	}

	private void unsubscribeAll(String user, Long chatId) {
		Map<String, ScheduledFuture<?>> subreddditSubscriptions = userSubscriptions.get(user);
		if (subreddditSubscriptions == null || subreddditSubscriptions.isEmpty()) {
			postMsg(chatId, "Not subscribed to any subreddit");
		} else {
			Set<String> subreddits = subreddditSubscriptions.keySet();
			Iterator<String> itr = subreddits.iterator();
			StringBuilder builder = new StringBuilder();
			builder.append("Successfully unsubscribed from ");
			while (itr.hasNext()) {
				String subreddit = itr.next();
				subreddditSubscriptions.get(subreddit).cancel(false);
				builder.append(subreddit + ", ");
			}
			builder.delete(builder.length() - 2, builder.length() - 1);
			userSubscriptions.put(user, new HashMap<>());
			postMsg(chatId, builder.toString());
		}
	}

	private void listSubscrption(String chatUserName, Long chatId) {
		Map<String, ScheduledFuture<?>> subredditSubscriptions = userSubscriptions.get(chatUserName);
		if (subredditSubscriptions == null || subredditSubscriptions.isEmpty()) {
			postMsg(chatId, "Not subscribed to any subreddit yet 😲");
		} else {
			final StringBuilder builder = new StringBuilder();
			builder.append("*🕵 Subreddits you are subscribed to :* \n\n");
			int count = 0;
			Iterator<String> itr = subredditSubscriptions.keySet().iterator();
			while (itr.hasNext()) {
				builder.append(itr.next() + ", ");
				count++;
			}
			builder.delete(builder.length() - 2, builder.length() - 1);
			builder.append("\nTotal subreddit subscription - " + count);
			postMsg(chatId, builder.toString());
		}
	}

	private void subscribe(String chatUserName, Long chatId, String subreddit, int frequency) {
		if (checkSubscriptionStatus(chatId, chatUserName, subreddit)) {
			ScheduledFuture<?> future = ResourceUtil.executor.scheduleAtFixedRate(() -> sendMessage(chatId, subreddit), 1, frequency,
						TimeUnit.HOURS);
			addUserSubcription(chatUserName, future, subreddit);
		}
	}

	private void addUserSubcription(String user, ScheduledFuture<?> future, String subreddit) {
		Map<String, ScheduledFuture<?>> subredditSubscriptions;
		if (userSubscriptions.get(user) != null) {
			subredditSubscriptions = userSubscriptions.get(user);
			subredditSubscriptions.put(subreddit, future);

		} else {
			subredditSubscriptions = new HashMap<>();
			subredditSubscriptions.put(subreddit, future);
			userSubscriptions.put(user, subredditSubscriptions);
		}
	}

	private void removeUserSubcription(String user, String subreddit) {
		Map<String, ScheduledFuture<?>> subredditSubscriptions = userSubscriptions.get(user);
		ScheduledFuture<?> future = subredditSubscriptions.get(subreddit);
		future.cancel(false);
		subredditSubscriptions.remove(subreddit);
		userSubscriptions.put(user, subredditSubscriptions);
	}

	private void sendMessage(Long chatId, String subreddit) {

		List<String> posts = RedditUtil.getTopPost(subreddit, 5);
		if (!posts.isEmpty()) {
			StringBuilder builder = new StringBuilder();
			builder.append("Top " + subreddit + " posts : ");
			for (int i = 1; i <= posts.size(); i++) {
				builder.append(i + ". " + posts.get(i));
			}
			postMsg(chatId, builder.toString());
		}
	}

	private boolean validateSubscribeCommand(long chatId, String command) {
		String[] parts = command.split(" ");
		if (parts.length != 3) {
			postMsg(chatId,
					"Invalid inputs.\n\n*Syntax is :* `subscribe <subreddit> <frequency in hours>`\n\ne.g. `subscribe news 12`\n\nIt will fetch news subreddit's top posts in every 12 hours.");
			return false;
		}
		return true;
	}

	private boolean validateUnsubscribeCommand(long chatId, String command) {
		String[] parts = command.split(" ");

		if (parts.length != 2) {
			postMsg(chatId, "Invalid inputs.\n\n*Syntax is :* `unsubscribe <subreddit>`\n\ne.g. `unsubscribe news`");
			return false;
		}
		return true;
	}

	private void initPost(Long chatId) {
		StringBuilder builder = new StringBuilder();
		builder.append("You can use this bot to subscribe to subreddit's top posts.\n\n");
		builder.append("Commands:\n");
		builder.append("*subscribe* - to subscribe to a particular subreddit\n");
		builder.append("*unsubscribe* - to unsubscribe from a particular subreddit\n");
		builder.append("*list* - to see all subscriptions\n");
		builder.append("*status* - to check status of the bot");
		postMsg(chatId, builder.toString());
	}

	private boolean checkSubscriptionStatus(Long chatId, String user, String subreddit) {
		if (isUserSubscriptionExist(user, subreddit)) {
			String msg = "Already subscribed for " + subreddit + "!";
			postMsg(chatId, msg);
			return false;
		}
		List<String> posts = RedditUtil.getTopPost(subreddit, 5);

		if (posts.isEmpty()) {
			String msg = "Either subreddit " + subreddit + " doesn't exist or inactive 🙁";
			postMsg(chatId, msg);
			return false;
		} else {
			String msg = "Successfully subscribed to subreddit " + subreddit + " 🙃";
			postMsg(chatId, msg);
		}
		return true;
	}

	private void postMsg(long chatId, String reply) {
		System.out.println(reply);
		SendMessage message = new SendMessage().setChatId(chatId).setText(reply).enableMarkdown(true);
		try {
			execute(message);
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
	}
}
