package pro.sky.telegrambot.handler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.sky.telegrambot.model.Client;
import pro.sky.telegrambot.model.Pet;
import pro.sky.telegrambot.service.VolunteerService;

import java.util.HashMap;
import java.util.Map;

@Component
public class VolunteerRequestHandler implements CommandHandler {

    private final Logger logger = LoggerFactory.getLogger(VolunteerRequestHandler.class);
    private final TelegramBot telegramBot;
    private final VolunteerService volunteerService;
    private final Map<Long, Long> pendingRejections = new HashMap<>();

    @Autowired
    public VolunteerRequestHandler(TelegramBot telegramBot, VolunteerService volunteerService) {
        this.telegramBot = telegramBot;
        this.volunteerService = volunteerService;
    }

    @Override
    public boolean canHandle(String command) {
        if (command == null) {
            return false;
        }

        if (command.startsWith("approve_request") || command.startsWith("reject_request")) {
            return true;
        }

        try {
            Long.parseLong(command);
            return pendingRejections.containsKey(Long.parseLong(command));
        } catch (NumberFormatException e) {
            logger.warn("Received non-numeric command: {}", command);
            return false;
        }
    }

    @Override
    public void handle(Message message) {

    }

    @Override
    public void handleCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.data();
        Long clientId = null;

        try {
            clientId = Long.parseLong(data.split(":")[1]);
        } catch (NumberFormatException e) {
            logger.error("Invalid client ID in callback data: {}", data);
            return;
        }

        if (data.startsWith("approve_request")) {
            approveRequest(clientId);
        } else if (data.startsWith("reject_request")) {
            rejectRequest(clientId);
        } else if (data.startsWith("pet_received")) {
            startReportProcess(clientId);
        } else if (data.startsWith("confirm_transfer")) {
            confirmTransfer(clientId);
        }
    }

//    private void approveRequest(Long clientId) {
//        Client client = volunteerService.getClientById(clientId);
//        if (client != null) {
//            telegramBot.execute(new SendMessage(client.getChatId(), "Ваша заявка была одобрена! Вы можете забрать питомца."));
//            logger.info("Sent approval message to client with ID: {}", clientId);
//            closeRequest(clientId);
//        } else {
//            logger.error("Client with ID {} not found", clientId);
//        }
//    }

    private void rejectRequest(Long clientId) {
        Client client = volunteerService.getClientById(clientId);
        if (client != null) {
            // Уведомляем клиента об отказе
            telegramBot.execute(new SendMessage(client.getChatId(), "Вам отказано."));
            logger.info("Sent rejection message to client with ID: {}", clientId);
            // Закрываем заявку
            closeRequest(clientId);
        } else {
            logger.error("Client with ID {} not found", clientId);
        }
    }

    private void closeRequest(Long clientId) {
        volunteerService.deleteClientById(clientId);
        logger.info("Closed request for client with ID: {}", clientId);
    }

    private void approveRequest(Long clientId) {
        Client client = volunteerService.getClientById(clientId);
        if (client != null) {
            // Шаг 1: Уведомляем клиента об одобрении заявки
            telegramBot.execute(new SendMessage(client.getChatId(), "Ваша заявка была одобрена! Вы можете забрать питомца."));

            Pet pet = client.getAdoptedPet();
            String petInfo = String.format("Ваш питомец:\n\nКличка: %s\nПорода: %s", pet.getName(), pet.getBreed());

            // Отправляем анкету питомца и кнопку "Получил питомца"
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(
                    new InlineKeyboardButton("Получил питомца").callbackData("pet_received:" + client.getId())
            );

            telegramBot.execute(new SendMessage(client.getChatId(), petInfo).replyMarkup(keyboard));

            // Уведомляем волонтера
            notifyVolunteer(client, pet);
        } else {
            logger.error("Client with ID {} not found", clientId);
        }
    }

    private void notifyVolunteer(Client client, Pet pet) {
        Long volunteerChatId = pet.getVolunteer().getChatId();
        String clientInfo = String.format("Клиент: %s\nВозраст: %d\nТелефон: %s\n\nВзял питомца: %s (%s)",
                client.getName(), client.getAge(), client.getPhoneNumber(), pet.getName(), pet.getBreed());

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton("Подтвердить передачу").callbackData("confirm_transfer:" + client.getId())
        );

        telegramBot.execute(new SendMessage(volunteerChatId, clientInfo).replyMarkup(keyboard));
    }

    private void startReportProcess(Long clientId) {
        Client client = volunteerService.getClientById(clientId);
        if (client != null) {
            volunteerService.startReportProcess(client);
            telegramBot.execute(new SendMessage(client.getChatId(), "Отлично! Ожидайте уведомлений для отправки отчетов."));
        }
    }

    private void confirmTransfer(Long clientId) {
        Client client = volunteerService.getClientById(clientId);
        if (client != null) {
            telegramBot.execute(new SendMessage(client.getChatId(), "Передача питомца подтверждена!"));
        }
    }
}