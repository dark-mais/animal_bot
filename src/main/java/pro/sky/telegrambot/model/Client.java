package pro.sky.telegrambot.model;

import javax.persistence.*;
import java.util.List;

@Entity
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int age;
    private String phoneNumber;

    @OneToOne
    @JoinColumn(name = "pet_id")
    private Pet adoptedPet;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Pet getAdoptedPet() {
        return adoptedPet;
    }

    public void setAdoptedPet(Pet adoptedPet) {
        this.adoptedPet = adoptedPet;
    }
}
