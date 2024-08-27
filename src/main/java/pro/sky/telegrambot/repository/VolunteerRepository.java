package pro.sky.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pro.sky.telegrambot.model.Volunteer;

public interface VolunteerRepository extends JpaRepository<Volunteer, Long> {

    @Query("SELECT v FROM Volunteer v LEFT JOIN FETCH v.pets WHERE v.chatId = :chatId")
    Volunteer findByChatId(@Param("chatId") Long chatId);
}