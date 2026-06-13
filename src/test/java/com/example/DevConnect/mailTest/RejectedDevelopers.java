package com.example.DevConnect.mailTest;

import com.example.DevConnect.entity.User;
import com.example.DevConnect.repository.ApplicationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class RejectedDevelopers {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Test
    public void check() {
        List<User> rejectedDevelopers = applicationRepository.getRejectedDevelopers();
        rejectedDevelopers.forEach(System.out::println);
    }

}
