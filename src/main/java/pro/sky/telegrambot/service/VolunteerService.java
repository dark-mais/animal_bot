package pro.sky.telegrambot.service;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.Client;
import pro.sky.telegrambot.model.Pet;
import pro.sky.telegrambot.model.Volunteer;
import pro.sky.telegrambot.repository.ClientRepository;
import pro.sky.telegrambot.repository.PetRepository;
import pro.sky.telegrambot.repository.VolunteerRepository;

import javax.transaction.Transactional;
import java.util.List;

@Service
public class VolunteerService {

    private final Logger logger = LoggerFactory.getLogger(VolunteerService.class);
    private final VolunteerRepository volunteerRepository;
    private final PetRepository petRepository;
    private final ClientRepository clientRepository;

    @Autowired
    public VolunteerService(VolunteerRepository volunteerRepository, PetRepository petRepository, ClientRepository clientRepository) {
        this.volunteerRepository = volunteerRepository;
        this.petRepository = petRepository;
        this.clientRepository = clientRepository;
    }

    // Метод для проверки, зарегистрирован ли волонтер
    public boolean isVolunteerRegistered(Long chatId) {
        return volunteerRepository.findByChatId(chatId) != null;
    }

    // Метод для регистрации волонтера
    public Volunteer registerVolunteer(Long chatId, String name) {
        if (isVolunteerRegistered(chatId)) {
            return null;  // Возвращаем null, если волонтер уже зарегистрирован
        }

        Volunteer volunteer = new Volunteer();
        volunteer.setChatId(chatId);
        volunteer.setName(name);
        return volunteerRepository.save(volunteer);
    }

    // Метод для добавления питомца волонтеру
    public Pet addPetToVolunteer(Long chatId, String name, String breed, String photoFileId) {
        Volunteer volunteer = volunteerRepository.findByChatId(chatId);
        if (volunteer == null) {
            return null;
        }

        Pet pet = new Pet();
        pet.setName(name);
        pet.setBreed(breed);
        pet.setPhotoFileId(photoFileId);
        pet.setVolunteer(volunteer);
        return petRepository.save(pet);
    }

    // Метод для получения списка питомцев волонтера
    public List<Pet> getPetsByVolunteer(Long chatId) {
        Volunteer volunteer = volunteerRepository.findByChatId(chatId);
        return volunteer != null ? volunteer.getPets() : List.of();
    }

    // Метод для удаления питомца
    public void deletePet(Long petId) {
        petRepository.deleteById(petId);
    }

    public List<Pet> getAllPets() {
        return petRepository.findAll();
    }

    public Pet getPetById(Long petId) {
        return petRepository.findById(petId).orElse(null);
    }

    public Client saveClient(Client client) {
        return clientRepository.save(client);
    }
}