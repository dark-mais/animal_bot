package pro.sky.telegrambot.handler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StartCommandHandler implements CommandHandler {

    private final TelegramBot telegramBot;

    @Autowired
    public StartCommandHandler(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    @Override
    public boolean canHandle(String command) {
        return command.equals("/start");
    }

    @Override
    public void handle(Message message) {
        Long chatId = message.chat().id();
        String welcomeMessage = "Привет! Добро пожаловать в нашего бота. Выберите действие из меню.";

        // Создание меню с кнопкой "Выбрать питомца"
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup(
                new String[]{"Выбрать питомца"})
                .resizeKeyboard(true)
                .oneTimeKeyboard(false);

        // Отправка приветственного сообщения с меню
        telegramBot.execute(new SendMessage(chatId, welcomeMessage).replyMarkup(keyboard));
    }

    @Override
    public void handleCallbackQuery(CallbackQuery callbackQuery) {

    }
}