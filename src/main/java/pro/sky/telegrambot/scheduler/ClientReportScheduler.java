package pro.sky.telegrambot.scheduler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pro.sky.telegrambot.model.Client;
import pro.sky.telegrambot.service.VolunteerService;

import java.util.List;

@Component
public class ClientReportScheduler {

    private final VolunteerService volunteerService;
    private final TelegramBot telegramBot;

    @Autowired
    public ClientReportScheduler(VolunteerService volunteerService, TelegramBot telegramBot) {
        this.volunteerService = volunteerService;
        this.telegramBot = telegramBot;
    }

    @Scheduled(fixedRate = 60000) // Запуск каждые 60 секунд
    public void requestReports() {
        List<Client> clients = volunteerService.getClientsAwaitingReports();
        for (Client client : clients) {
            if (client.getReportCount() < 3) {
                telegramBot.execute(new SendMessage(client.getChatId(), "Пожалуйста, отправьте отчет с фотографией."));
            }
        }
    }
}
