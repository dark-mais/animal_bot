package pro.sky.telegrambot.handler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.PhotoSize;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.sky.telegrambot.model.Client;
import pro.sky.telegrambot.service.VolunteerService;

@Component
public class ReportHandler {

    private final Logger logger = LoggerFactory.getLogger(ReportHandler.class);
    private final TelegramBot telegramBot;
    private final VolunteerService volunteerService;

    @Autowired
    public ReportHandler(TelegramBot telegramBot, VolunteerService volunteerService) {
        this.telegramBot = telegramBot;
        this.volunteerService = volunteerService;
    }

    public void handleReport(Message message) {
        Long chatId = message.chat().id();
        Client client = volunteerService.getClientByChatId(chatId);

        if (client != null && message.photo() != null) {
            PhotoSize photo = message.photo()[message.photo().length - 1];
            String fileId = photo.fileId();

            // Увеличиваем счетчик отчетов
            client.incrementReportCount();
            volunteerService.saveClient(client);

            // Пересылаем фото волонтеру
            Long volunteerChatId = client.getAdoptedPet().getVolunteer().getChatId();
            telegramBot.execute(new SendPhoto(volunteerChatId, fileId)
                    .caption("Отчет от клиента " + client.getName()));

            // Проверка завершения отчетов
            if (client.getReportCount() >= 3) {
                telegramBot.execute(new SendMessage(volunteerChatId, "Все отчеты получены. Оцените последний отчет."));
            } else {
                telegramBot.execute(new SendMessage(chatId, "Отчет принят. Ожидайте следующего запроса."));
            }
        }
    }
}