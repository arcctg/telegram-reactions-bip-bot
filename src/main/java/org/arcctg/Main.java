package org.arcctg;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

public class Main {

    public static final String BOT_TOKEN = "7638117735:AAF6EA41J8cS-ACo9UKoKUyy1rk_jednvwc";

    public static void main(String[] args) {
        try (TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication()) {
            botsApplication.registerBot(BOT_TOKEN , new ReactionBot(BOT_TOKEN));
            System.out.println("Bot started");
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}