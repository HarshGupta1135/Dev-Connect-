package com.example.DevConnect.repository;

import com.example.DevConnect.entity.DeveloperProfile;
import com.example.DevConnect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeveloperProfileRepository extends JpaRepository<DeveloperProfile, Long> {
    Optional<DeveloperProfile> findByUser(User user);
}
