package org.arcctg;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.arcctg.database.DatabaseManager;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.reactions.SetMessageReaction;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.reactions.ReactionTypeEmoji;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class ReactionBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;

    public static final String BOT_TOKEN = System.getenv("BOT_TOKEN");

    public ReactionBot() {
        this.telegramClient = new OkHttpTelegramClient(getBotToken());
        DatabaseManager.initializeDatabase();
        setBotCommands();
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @AfterBotRegistration
    public void afterRegistration(BotSession botSession) {
        System.out.println("Registered bot running state is: " + botSession.isRunning());
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();

            if (messageText.startsWith("/")) {
                String[] parts = messageText.split(" ");
                String command = parts[0];

                if (command.equals("/adduser")) {
                    if (parts.length < 2) {
                        System.out.println("Expected at least the username");
                    } else if (parts.length < 3) {
                        addUser(parts[1], "🤡");
                    } else {
                        addUser(parts[1], parts[2]);
                    }
                } else if (command.equals("/updateuser")) {
                    if (parts.length < 3) {
                        System.out.println("Expected the username and the new emoji");
                    } else {
                        updateUser(parts[1], parts[2]);
                    }
                } else if (command.equals("/removeuser")) {
                    if (parts.length < 2) {
                        System.out.println("Expected the username");
                    } else {
                        removeUser(parts[1]);
                    }
                } else {
                    System.out.println("Unknown command: " + command);
                }
            } else {
                try {
                    String username = update.getMessage().getFrom().getUserName();
                    String emoji = getEmojiForUsername(username);

                    if (emoji != null) {
                        setReactionOnMessage(update.getMessage(), emoji);
                    }
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void removeUser(String username) {
        try (Connection connection = DatabaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement("DELETE FROM users WHERE username = ?")) {

            statement.setString(1, username);

            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateUser(String username, String emoji) {
        try (Connection connection = DatabaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement("UPDATE users SET emoji = ? WHERE username = ?")) {

            statement.setString(1, emoji);
            statement.setString(2, username);

            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addUser(String username, String emoji) {
        try (Connection connection = DatabaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement("INSERT INTO users (username, emoji) VALUES (?, ?)")) {

            statement.setString(1, username);
            statement.setString(2, emoji);

            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getEmojiForUsername(String username) {
        try (Connection connection = DatabaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement("SELECT emoji FROM users WHERE username = ?")) {
            statement.setString(1, username);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString("emoji");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void setReactionOnMessage(Message message, String emoji) throws TelegramApiException {
        telegramClient.executeAsync(SetMessageReaction.builder()
            .chatId(message.getChatId())
            .messageId(message.getMessageId())
            .reactionTypes(List.of(
                ReactionTypeEmoji.builder()
                    .emoji(emoji)
                    .build()
            ))
            .build());
    }

    private void setBotCommands() {
        List<BotCommand> commandList = new ArrayList<>();
        commandList.add(new BotCommand("/adduser", "Add a new user with an emoji. Usage: /adduser <username> <emoji>. Emoji is optional, default is 🤡"));

        SetMyCommands setMyCommands = SetMyCommands.builder()
            .commands(commandList)
            .scope(new BotCommandScopeDefault())
            .build();

        try {
            telegramClient.execute(setMyCommands);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
