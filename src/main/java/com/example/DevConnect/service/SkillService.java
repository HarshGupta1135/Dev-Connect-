package com.example.DevConnect.service;

import com.example.DevConnect.dto.request.SkillRequest;
import com.example.DevConnect.entity.Skill;
import com.example.DevConnect.exception.BadRequestException;
import com.example.DevConnect.exception.DuplicateResourceException;
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

    @Transactional(readOnly = true)
    public List<Skill> getAllSkills() {
        return skillRepository.findAll();
    }

    @Transactional
    public List<Skill> addSkills(List<SkillRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException("Skills list cannot be empty");
        }

        List<String> names = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (SkillRequest request : requests) {
            if (request == null || request.getName() == null || request.getName().isBlank()) {
                throw new BadRequestException("Skill name cannot be empty");
            }
            String name = request.getName().trim();
            if (!seen.add(name.toLowerCase())) {
                throw new BadRequestException("Duplicate skill name in request list: " + name);
            }
            names.add(name);
        }

        // One query for the whole batch instead of one per skill.
        List<Skill> existing = skillRepository.findByNameIn(names);
        if (!existing.isEmpty()) {
            List<String> existingNames = existing.stream().map(Skill::getName).toList();
            throw new DuplicateResourceException("Skill already exists with this name : " + String.join(", ", existingNames));
        }

        List<Skill> toSave = new ArrayList<>();
        for (String name : names) {
            Skill skill = new Skill();
            skill.setName(name);
            toSave.add(skill);
        }

        return skillRepository.saveAll(toSave);
    }
}
