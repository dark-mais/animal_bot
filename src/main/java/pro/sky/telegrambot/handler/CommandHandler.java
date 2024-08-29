package pro.sky.telegrambot.handler;

import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;

public interface CommandHandler {
    boolean canHandle(String command);
    void handle(Message message);
    void handleCallbackQuery(CallbackQuery callbackQuery);
}