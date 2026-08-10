package com.example.DevConnect.controller;

import com.example.DevConnect.dto.request.SkillRequest;
import com.example.DevConnect.dto.response.ApiResponse;
import com.example.DevConnect.entity.Skill;
import com.example.DevConnect.service.SkillService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Validated
@Tag(name = "Skills Management", description = "Endpoints for retrieving and adding skills")
public class SkillController {

    @Autowired
    private SkillService skillService;

    @GetMapping("/get/all/skills")
    @Operation(summary = "Get all skills", description = "Retrieves all available skills in the database.")
    public ResponseEntity<?> getAllSkills(){
        List<Skill> allSkills = skillService.getAllSkills();
        return ResponseEntity.ok(ApiResponse.success("Skills Retrieved Successfully", allSkills));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add/skills")
    @Operation(summary = "Add skills (Admin Only)", description = "Allows administrators to seed the system with new skills.")
    public ResponseEntity<?> addSkills(@RequestBody @Valid List<@Valid SkillRequest> skills){
        List<Skill> added = skillService.addSkills(skills);
        return ResponseEntity.ok(ApiResponse.success("Skills Added Successfully", added));
    }

}
