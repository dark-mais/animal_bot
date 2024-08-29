package pro.sky.telegrambot.handler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove;
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
        return command != null && (command.equals("Выбрать питомца")
                || command.startsWith("select_pet")
                || command.startsWith("adopt_pet")
                || !registrationStates.isEmpty());
    }

    @Override
    public void handle(Message message) {
        Long chatId = message.chat().id();
        String text = message.text();

        // Проверяем, есть ли уже активная заявка у клиента
        if (volunteerService.isClientPending(chatId)) {
            telegramBot.execute(new SendMessage(chatId, "Ваша заявка уже рассматривается. Ожидайте решения волонтера."));
            return;
        }

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
                        telegramBot.execute(new SendMessage(chatId, "Возраст должен быть числом. Пожалуйста, укажите ваш возраст."));
                    }
                    break;
                case "AWAITING_PHONE":
                    client.setPhoneNumber(text.trim());
                    Pet pet = client.getAdoptedPet();
                    client.setAdoptedPet(pet);
                    volunteerService.saveClient(client);

                    // Уведомление волонтеру
                    sendAdoptionRequestToVolunteer(client, pet);

                    // Завершение регистрации
                    registrationStates.remove(chatId);
                    pendingRegistrations.remove(chatId);

                    // Возвращаем клиенту клавиатуру
                    ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup(
                            new String[]{"Выбрать питомца"})
                            .resizeKeyboard(true)
                            .oneTimeKeyboard(false);

                    telegramBot.execute(new SendMessage(chatId, "Регистрация завершена. Ожидайте решения волонтера.").replyMarkup(keyboard));
                    break;
                default:
                    telegramBot.execute(new SendMessage(chatId, "Произошла ошибка. Попробуйте снова."));
                    registrationStates.remove(chatId);
                    pendingRegistrations.remove(chatId);
                    break;
            }
        } else {
            if (text.equals("Выбрать питомца")) {
                showPet(chatId, 0);  // Показываем первого питомца
            } else {
                telegramBot.execute(new SendMessage(chatId, "Пожалуйста, сначала нажмите 'Выбрать питомца' и следуйте инструкциям."));
            }
        }
    }

    private void sendAdoptionRequestToVolunteer(Client client, Pet pet) {
        Long volunteerChatId = pet.getVolunteer().getChatId(); // Получаем chatId волонтера
        String messageText = String.format("Поступила новая заявка на усыновление питомца:\n\nКлиент: %s\nВозраст: %d\nТелефон: %s\n\nПитомец: %s (%s)",
                client.getName(), client.getAge(), client.getPhoneNumber(), pet.getName(), pet.getBreed());

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton("Одобрить").callbackData("approve_request:" + client.getId()),
                new InlineKeyboardButton("Отказать").callbackData("reject_request:" + client.getId())
        );

        telegramBot.execute(new SendMessage(volunteerChatId, messageText).replyMarkup(keyboard));
    }

    public void handleCallbackQuery(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.message().chat().id();
        String data = callbackQuery.data();

        if (data.startsWith("select_pet")) {
            int petIndex = Integer.parseInt(data.split(":")[1]);
            showPet(chatId, petIndex);
        } else if (data.startsWith("adopt_pet")) {
            int petIndex = Integer.parseInt(data.split(":")[1]);
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
    }

    private void startClientRegistration(Long chatId, Pet pet) {
        Client client = new Client();
        client.setAdoptedPet(pet);
        client.setChatId(chatId);  // Сохраняем chatId клиента
        pendingRegistrations.put(chatId, client);
        registrationStates.put(chatId, "AWAITING_NAME");
        telegramBot.execute(new SendMessage(chatId, "Пожалуйста, укажите ваше имя для регистрации.").replyMarkup(new ReplyKeyboardRemove()));
    }
}