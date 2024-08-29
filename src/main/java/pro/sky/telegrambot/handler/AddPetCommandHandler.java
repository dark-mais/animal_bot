package pro.sky.telegrambot.handler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.PhotoSize;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.sky.telegrambot.model.Pet;
import pro.sky.telegrambot.service.VolunteerService;

import java.util.HashMap;
import java.util.Map;

@Component
public class AddPetCommandHandler implements CommandHandler {

    private final Logger logger = LoggerFactory.getLogger(AddPetCommandHandler.class);
    private final VolunteerService volunteerService;
    private final TelegramBot telegramBot;

    private final Map<Long, String> userStates = new HashMap<>();
    private final Map<Long, Pet> tempPetData = new HashMap<>();

    @Autowired
    public AddPetCommandHandler(VolunteerService volunteerService, TelegramBot telegramBot) {
        this.volunteerService = volunteerService;
        this.telegramBot = telegramBot;
    }

    @Override
    public boolean canHandle(String command) {
        return command !=null && command.equals("Добавить питомца") ||
                !userStates.isEmpty();
    }

    @Override
    public void handle(Message message) {
        Long chatId = message.chat().id();
        String text = message.text();

        if (text != null && text.equals("Добавить питомца")) {
            startPetAddition(chatId);
        } else if (userStates.containsKey(chatId) && "AWAITING_PET_NAME".equals(userStates.get(chatId))) {
            handlePetName(chatId, text);
        } else if (userStates.containsKey(chatId) && "AWAITING_PET_BREED".equals(userStates.get(chatId))) {
            handlePetBreed(chatId, text);
        } else if (userStates.containsKey(chatId) && "AWAITING_PET_PHOTO".equals(userStates.get(chatId))) {
            handlePetPhoto(chatId, message);
        }
    }

    @Override
    public void handleCallbackQuery(CallbackQuery callbackQuery) {

    }

    private void startPetAddition(Long chatId) {
        telegramBot.execute(new SendMessage(chatId, "Пожалуйста, отправьте кличку питомца."));
        userStates.put(chatId, "AWAITING_PET_NAME");
    }

    private void handlePetName(Long chatId, String petName) {
        Pet tempPet = new Pet();
        tempPet.setName(petName.trim());
        tempPetData.put(chatId, tempPet);

        telegramBot.execute(new SendMessage(chatId, "Пожалуйста, отправьте породу питомца."));
        userStates.put(chatId, "AWAITING_PET_BREED");
    }

    private void handlePetBreed(Long chatId, String petBreed) {
        Pet tempPet = tempPetData.get(chatId);
        tempPet.setBreed(petBreed.trim());

        telegramBot.execute(new SendMessage(chatId, "Пожалуйста, отправьте фото питомца."));
        userStates.put(chatId, "AWAITING_PET_PHOTO");
    }

    private void handlePetPhoto(Long chatId, Message message) {
        if (message.photo() != null && message.photo().length > 0) {
            PhotoSize photo = message.photo()[message.photo().length - 1];
            String fileId = photo.fileId();

            Pet tempPet = tempPetData.get(chatId);
            tempPet.setPhotoFileId(fileId);

            // Сохраняем питомца
            volunteerService.addPetToVolunteer(chatId, tempPet.getName(), tempPet.getBreed(), tempPet.getPhotoFileId());

            telegramBot.execute(new SendMessage(chatId, "Питомец успешно добавлен!"));

            // Очищаем состояние и временные данные
            userStates.remove(chatId);
            tempPetData.remove(chatId);
        } else {
            telegramBot.execute(new SendMessage(chatId, "Пожалуйста, отправьте корректное фото питомца."));
        }
    }
}