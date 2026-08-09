package com.example.DevConnect.service;

import com.example.DevConnect.entity.Skill;
import com.example.DevConnect.repository.SkillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SkillService {

    @Autowired
    private SkillRepository skillRepository;

    public List<Skill> getAllSkills() {
        return skillRepository.findAll();
    }

    @Transactional
    public List<Skill> addSkills(List<Skill> skills) {
        if (skills == null || skills.isEmpty()) {
            throw new IllegalArgumentException("Skills list cannot be empty");
        }

        Set<String> seenNames = new HashSet<>();
        for (Skill skill : skills) {
            if (skill.getName() == null || skill.getName().isBlank()) {
                throw new IllegalArgumentException("Skill name cannot be empty");
            }
            String skillName = skill.getName().trim();
            if (seenNames.contains(skillName)) {
                throw new RuntimeException("Duplicate skill name in request list: " + skillName);
            }
            if (skillRepository.findByName(skillName).isPresent()) {
                throw new RuntimeException("Skill already exists with this name : " + skillName);
            }
            seenNames.add(skillName);
            skill.setName(skillName);
        }
        return skillRepository.saveAll(skills);
    }
}
