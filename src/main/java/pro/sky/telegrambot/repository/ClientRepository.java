package pro.sky.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.sky.telegrambot.model.Client;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Client findByChatId(Long chatId);
    // Метод для проверки, существует ли клиент с данным chatId
    boolean existsByChatId(Long chatId);
}