package com.example.DevConnect.controller;

import com.example.DevConnect.entity.User;
import com.example.DevConnect.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-all-users")
    public List<User> getAllUsers() {
        return adminService.getAllUsers();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add-admin")
    public ResponseEntity<?> createAdmin(@RequestBody User user){
        return adminService.createAdmin(user);
    }

}
