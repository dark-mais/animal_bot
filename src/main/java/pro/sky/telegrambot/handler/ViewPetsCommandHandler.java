package pro.sky.telegrambot.handler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.sky.telegrambot.model.Pet;
import pro.sky.telegrambot.service.VolunteerService;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Component
public class ViewPetsCommandHandler implements CommandHandler {

    private final Logger logger = LoggerFactory.getLogger(ViewPetsCommandHandler.class);
    private final VolunteerService volunteerService;
    private final TelegramBot telegramBot;

    private final Map<Long, Integer> userCurrentPetIndex = new HashMap<>();

    @Autowired
    public ViewPetsCommandHandler(VolunteerService volunteerService, TelegramBot telegramBot) {
        this.volunteerService = volunteerService;
        this.telegramBot = telegramBot;
    }

    @Override
    public boolean canHandle(String command) {
        return command.equals("Посмотреть питомцев") || command.startsWith("view_pet") || command.startsWith("delete_pet");
    }

    @Override
    public void handle(Message message) {
        Long chatId = message.chat().id();
        showPet(chatId, 0);  // Начинаем с первого питомца
    }

    public void handleCallbackQuery(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.message().chat().id();
        String data = callbackQuery.data();

        if (data.startsWith("view_pet")) {
            int petIndex = Integer.parseInt(data.split(":")[1]);
            showPet(chatId, petIndex);
        } else if (data.startsWith("delete_pet")) {
            int petIndex = Integer.parseInt(data.split(":")[1]);
            deletePet(chatId, petIndex);
        }
    }

    private void showPet(Long chatId, int petIndex) {
        List<Pet> pets = volunteerService.getPetsByVolunteer(chatId);

        if (pets.isEmpty()) {
            telegramBot.execute(new SendMessage(chatId, "У вас нет добавленных питомцев."));
            return;
        }

        if (petIndex < 0 || petIndex >= pets.size()) {
            telegramBot.execute(new SendMessage(chatId, "Вы просмотрели всех питомцев."));
            return;
        }

        Pet pet = pets.get(petIndex);
        userCurrentPetIndex.put(chatId, petIndex);

        String petInfo = String.format("Питомец %d из %d\n\nКличка: %s\nПорода: %s",
                petIndex + 1, pets.size(), pet.getName(), pet.getBreed());

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton("Удалить").callbackData("delete_pet:" + petIndex),
                new InlineKeyboardButton("Далее").callbackData("view_pet:" + (petIndex + 1))
        );

        // Отправка фото питомца
        if (pet.getPhotoFileId() != null) {
            SendPhoto sendPhoto = new SendPhoto(chatId, pet.getPhotoFileId())
                    .caption(petInfo)
                    .replyMarkup(keyboard);
            telegramBot.execute(sendPhoto);
        } else {
            SendMessage message = new SendMessage(chatId, petInfo)
                    .replyMarkup(keyboard);
            telegramBot.execute(message);
        }

        logger.info("Showing pet index {} to chatId: {}", petIndex, chatId);
    }

    private void deletePet(Long chatId, int petIndex) {
        List<Pet> pets = volunteerService.getPetsByVolunteer(chatId);

        if (petIndex < 0 || petIndex >= pets.size()) {
            telegramBot.execute(new SendMessage(chatId, "Невозможно удалить питомца. Неверный индекс."));
            return;
        }

        Pet pet = pets.get(petIndex);
        volunteerService.deletePet(pet.getId());

        telegramBot.execute(new SendMessage(chatId, "Питомец успешно удален."));
        logger.info("Deleted pet with id {} for chatId: {}", pet.getId(), chatId);

        // Показываем следующий питомец, если он есть
        if (pets.size() > 1) {
            showPet(chatId, petIndex);
        } else {
            telegramBot.execute(new SendMessage(chatId, "У вас больше нет питомцев."));
        }
    }
}