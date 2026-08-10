package com.example.DevConnect.mailTest;

import com.example.DevConnect.entity.User;
import com.example.DevConnect.repository.ApplicationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Checks that the rejected/shortlisted projection queries still execute (needs MySQL).
 */
@SpringBootTest
@Transactional(readOnly = true)
public class RejectedDevelopersTest {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Test
    public void projectionQueriesRun() {
        List<User> rejected = applicationRepository.getRejectedDevelopers();
        List<User> shortlisted = applicationRepository.getShortlistedDevelopers();

        assertNotNull(rejected);
        assertNotNull(shortlisted);
    }

}
