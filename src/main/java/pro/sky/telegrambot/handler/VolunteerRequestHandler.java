package pro.sky.telegrambot.handler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VolunteerRequestHandler implements CommandHandler {

    private final Logger logger = LoggerFactory.getLogger(VolunteerRequestHandler.class);
    private final TelegramBot telegramBot;
    //private final ClientService clientService;

    @Autowired
    public VolunteerRequestHandler(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
        //this.clientService = clientService;
    }

    @Override
    public boolean canHandle(String command) {
        return command.startsWith("approve_request") || command.startsWith("reject_request");
    }

    @Override
    public void handle(Message message) {
        // Пустая реализация, так как этот обработчик работает только с CallbackQuery
    }

    public void handleCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.data();
        Long clientChatId = Long.parseLong(data.split(":")[1]);

        if (data.startsWith("approve_request")) {
            telegramBot.execute(new SendMessage(clientChatId, "Ваша заявка была одобрена!"));
        } else if (data.startsWith("reject_request")) {
            telegramBot.execute(new SendMessage(clientChatId, "Ваша заявка была отклонена."));
        }
    }
}