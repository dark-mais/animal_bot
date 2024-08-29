package pro.sky.telegrambot.handler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.sky.telegrambot.model.Client;
import pro.sky.telegrambot.service.VolunteerService;

@Component
public class VolunteerReportEvaluationHandler {

    private final Logger logger = LoggerFactory.getLogger(VolunteerReportEvaluationHandler.class);
    private final TelegramBot telegramBot;
    private final VolunteerService volunteerService;

    @Autowired
    public VolunteerReportEvaluationHandler(TelegramBot telegramBot, VolunteerService volunteerService) {
        this.telegramBot = telegramBot;
        this.volunteerService = volunteerService;
    }

    public void handleEvaluation(CallbackQuery callbackQuery) {
        String data = callbackQuery.data();
        Long clientId = Long.parseLong(data.split(":")[1]);

        Client client = volunteerService.getClientById(clientId);
        if (client != null) {
            String evaluation = data.startsWith("good_report") ? "Отчет оценен как хороший" : "Отчет оценен как плохой";
            telegramBot.execute(new SendMessage(client.getChatId(), evaluation));
            telegramBot.execute(new SendMessage(callbackQuery.message().chat().id(), "Отчет клиента " + client.getName() + ": " + evaluation));
        }
    }
}
