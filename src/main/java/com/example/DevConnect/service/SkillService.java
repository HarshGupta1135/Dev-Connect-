package com.example.DevConnect.service;

import com.example.DevConnect.entity.Skill;
import com.example.DevConnect.repository.SkillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SkillService {

    @Autowired
    private SkillRepository skillRepository;

    public List<Skill> getAllSkills() {
        return skillRepository.findAll();
    }

    public void addSkills(Skill skills) {
        if(skillRepository.findByName(skills.getName()).isPresent()){
            throw new RuntimeException("Skill already exists with this name : " + skills.getName());
        }
        skillRepository.save(skills);
    }
}
