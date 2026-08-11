package com.example.DevConnect.controller;

import com.example.DevConnect.dto.request.AccountUpdateRequest;
import com.example.DevConnect.dto.response.AccountResponse;
import com.example.DevConnect.dto.response.ApiResponse;
import com.example.DevConnect.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Account", description = "Endpoints for the signed-in user's own login details")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/account/me")
    @Operation(summary = "Get the signed-in user's account",
            description = "Returns the username, login email and roles of the authenticated user. Available to any signed-in role.")
    public ResponseEntity<?> getAccount() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AccountResponse data = userService.getAccount(email);
        return ResponseEntity.ok(ApiResponse.success("Account Fetched Successfully", data));
    }

    @PutMapping("/account")
    @Operation(summary = "Update username and login email",
            description = "Updates the authenticated user's username and/or login email. Both must stay unique. "
                    + "When the email changes the response carries a replacement JWT, because the previous "
                    + "token was signed against the old address — clients must store it and drop the old one.")
    public ResponseEntity<?> updateAccount(@Valid @RequestBody AccountUpdateRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AccountResponse data = userService.updateAccount(email, request);
        return ResponseEntity.ok(ApiResponse.success("Account Updated Successfully", data));
    }
}
