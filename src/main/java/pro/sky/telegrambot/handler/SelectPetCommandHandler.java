package pro.sky.telegrambot.handler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.sky.telegrambot.model.Client;
import pro.sky.telegrambot.model.Pet;
import pro.sky.telegrambot.service.VolunteerService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SelectPetCommandHandler implements CommandHandler {

    private final Logger logger = LoggerFactory.getLogger(SelectPetCommandHandler.class);
    private final VolunteerService volunteerService;
    private final TelegramBot telegramBot;

    private final Map<Long, String> registrationStates = new HashMap<>();
    private final Map<Long, Client> pendingRegistrations = new HashMap<>();

    @Autowired
    public SelectPetCommandHandler(VolunteerService volunteerService, TelegramBot telegramBot) {
        this.volunteerService = volunteerService;
        this.telegramBot = telegramBot;
    }

    @Override
    public boolean canHandle(String command) {
        return command.equals("Выбрать питомца") || command.startsWith("select_pet") || command.startsWith("adopt_pet");
    }

    @Override
    public void handle(Message message) {
        Long chatId = message.chat().id();
        String text = message.text();

        logger.info("Handling message from chatId: {} with text: {}", chatId, text);

        if (registrationStates.containsKey(chatId)) {
            String currentState = registrationStates.get(chatId);
            Client client = pendingRegistrations.get(chatId);

            switch (currentState) {
                case "AWAITING_NAME":
                    client.setName(text.trim());
                    telegramBot.execute(new SendMessage(chatId, "Пожалуйста, укажите ваш возраст."));
                    registrationStates.put(chatId, "AWAITING_AGE");
                    break;
                case "AWAITING_AGE":
                    try {
                        int age = Integer.parseInt(text.trim());
                        client.setAge(age);
                        telegramBot.execute(new SendMessage(chatId, "Пожалуйста, укажите ваш номер телефона."));
                        registrationStates.put(chatId, "AWAITING_PHONE");
                    } catch (NumberFormatException e) {
                        logger.error("Invalid age input: {}", text);
                        telegramBot.execute(new SendMessage(chatId, "Возраст должен быть числом. Пожалуйста, укажите ваш возраст."));
                    }
                    break;
                case "AWAITING_PHONE":
                    client.setPhoneNumber(text.trim());
                    Pet pet = volunteerService.getPetById(client.getAdoptedPet().getId());
                    client.setAdoptedPet(pet);
                    volunteerService.saveClient(client);

                    telegramBot.execute(new SendMessage(chatId, "Регистрация завершена. Вы успешно взяли питомца: " + pet.getName()));
                    registrationStates.remove(chatId);
                    pendingRegistrations.remove(chatId);
                    break;
                default:
                    logger.error("Unknown state: {}", currentState);
                    telegramBot.execute(new SendMessage(chatId, "Произошла ошибка. Попробуйте снова."));
                    registrationStates.remove(chatId);
                    pendingRegistrations.remove(chatId);
                    break;
            }
        } else {
            // Если клиент снова нажал "Выбрать питомца", не начав регистрацию
            if (text.equals("Выбрать питомца")) {
                showPet(chatId, 0);  // Показываем первого питомца
            } else {
                logger.info("Ignoring unrelated message: {}", text);
                telegramBot.execute(new SendMessage(chatId, "Пожалуйста, сначала нажмите 'Взять' и следуйте инструкциям для завершения регистрации."));
            }
        }
    }

    public void handleCallbackQuery(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.message().chat().id();
        String data = callbackQuery.data();

        logger.info("Received callback data: {}", data);

        if (data.startsWith("select_pet")) {
            int petIndex = Integer.parseInt(data.split(":")[1]);
            logger.info("Handling 'select_pet' callback for pet index: {}", petIndex);
            showPet(chatId, petIndex);
        } else if (data.startsWith("adopt_pet")) {
            int petIndex = Integer.parseInt(data.split(":")[1]);
            logger.info("Handling 'adopt_pet' callback for pet index: {}", petIndex);
            Pet pet = volunteerService.getAllPets().get(petIndex);
            startClientRegistration(chatId, pet);
        }
    }

    private void showPet(Long chatId, int petIndex) {
        List<Pet> pets = volunteerService.getAllPets();

        if (pets.isEmpty()) {
            telegramBot.execute(new SendMessage(chatId, "Нет доступных питомцев."));
            return;
        }

        if (petIndex < 0 || petIndex >= pets.size()) {
            telegramBot.execute(new SendMessage(chatId, "Вы просмотрели всех питомцев."));
            return;
        }

        Pet pet = pets.get(petIndex);
        String petInfo = String.format("Питомец %d из %d\n\nКличка: %s\nПорода: %s",
                petIndex + 1, pets.size(), pet.getName(), pet.getBreed());

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton("Взять").callbackData("adopt_pet:" + petIndex),
                new InlineKeyboardButton("Далее").callbackData("select_pet:" + (petIndex + 1))
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

    private void startClientRegistration(Long chatId, Pet pet) {
        Client client = new Client();
        client.setAdoptedPet(pet);
        pendingRegistrations.put(chatId, client);
        registrationStates.put(chatId, "AWAITING_NAME");
        telegramBot.execute(new SendMessage(chatId, "Пожалуйста, укажите ваше имя для регистрации."));
    }
}