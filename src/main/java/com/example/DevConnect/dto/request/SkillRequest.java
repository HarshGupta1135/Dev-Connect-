package com.example.DevConnect.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Skill creation payload. Accepting the Skill entity itself allowed a client-supplied id
 * to rename an existing skill row.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SkillRequest {

    @NotBlank(message = "Skill name cannot be empty")
    @Size(max = 100, message = "Skill name must be at most 100 characters")
    private String name;
}
