package com.example.DevConnect.controller;

import com.example.DevConnect.dto.response.ApiResponse;
import com.example.DevConnect.entity.Skill;
import com.example.DevConnect.service.SkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SkillController {

    @Autowired
    private SkillService skillService;

    @GetMapping("/get/all/skills")
    public ResponseEntity<?> getAllSkills(){
        List<Skill> allSkills = skillService.getAllSkills();
        return ResponseEntity.ok(ApiResponse.success("Skills Retrived Successfully", allSkills));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add/skills")
    public ResponseEntity<?> addSkills(@RequestBody List<Skill> skills){
        List<Skill> added = skillService.addSkills(skills);
        return ResponseEntity.ok(ApiResponse.success("Skills Added Successfully", added));
    }

}
