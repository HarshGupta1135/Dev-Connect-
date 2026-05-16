package com.example.DevConnect.controller;

import com.example.DevConnect.entity.User;
import com.example.DevConnect.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.DevConnect.dto.response.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-all-users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", adminService.getAllUsers()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add-admin")
    public ResponseEntity<?> createAdmin(@RequestBody User user){
        ResponseEntity<?> serviceResponse = adminService.createAdmin(user);
        if (serviceResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(serviceResponse.getStatusCode())
                    .body(ApiResponse.success((String)serviceResponse.getBody(), null));
        } else {
            return ResponseEntity.status(serviceResponse.getStatusCode())
                    .body(ApiResponse.error((String)serviceResponse.getBody()));
        }
    }

}
