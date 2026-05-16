package com.example.DevConnect.repository;

import com.example.DevConnect.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUserName(String userName);
    Optional<User> findByEmail(String email);
}
