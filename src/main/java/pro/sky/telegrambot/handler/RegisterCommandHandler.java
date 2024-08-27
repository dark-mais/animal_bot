package pro.sky.telegrambot.handler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.sky.telegrambot.service.VolunteerService;

import java.util.HashMap;
import java.util.Map;

@Component
public class RegisterCommandHandler implements CommandHandler {

    private final Logger logger = LoggerFactory.getLogger(RegisterCommandHandler.class);
    private final VolunteerService volunteerService;
    private final TelegramBot telegramBot;

    private final Map<Long, String> userStates = new HashMap<>();

    @Autowired
    public RegisterCommandHandler(VolunteerService volunteerService, TelegramBot telegramBot) {
        this.volunteerService = volunteerService;
        this.telegramBot = telegramBot;
    }

    @Override
    public boolean canHandle(String command) {
        return command.startsWith("/register") || userStates.containsValue("AWAITING_NAME");
    }

    @Override
    public void handle(Message message) {
        Long chatId = message.chat().id();
        String text = message.text();

        // Логика для клиента и волонтера может различаться, в зависимости от контекста
        if (text.equalsIgnoreCase("Я клиент")) {
            sendClientMenu(chatId);
        } else if (volunteerService.isVolunteerRegistered(chatId)) {
            sendVolunteerMenu(chatId);
        } else if (userStates.containsKey(chatId) && "AWAITING_NAME".equals(userStates.get(chatId))) {
            volunteerService.registerVolunteer(chatId, text.trim());
            telegramBot.execute(new SendMessage(chatId, "Спасибо за регистрацию, " + text + "!"));
            userStates.remove(chatId);
            sendVolunteerMenu(chatId);
        } else {
            telegramBot.execute(new SendMessage(chatId, "Пожалуйста, отправьте свое имя для регистрации."));
            userStates.put(chatId, "AWAITING_NAME");
        }
    }

    @Override
    public void handleCallbackQuery(CallbackQuery callbackQuery) {

    }

    private void sendVolunteerMenu(Long chatId) {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup(
                new String[]{"Добавить питомца", "Посмотреть питомцев"})
                .resizeKeyboard(true)
                .oneTimeKeyboard(false);

        telegramBot.execute(new SendMessage(chatId, "Выберите действие:").replyMarkup(keyboard));
    }

    private void sendClientMenu(Long chatId) {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup(
                new String[]{"Выбрать питомца"})
                .resizeKeyboard(true)
                .oneTimeKeyboard(false);

        telegramBot.execute(new SendMessage(chatId, "Выберите действие:").replyMarkup(keyboard));
    }
}