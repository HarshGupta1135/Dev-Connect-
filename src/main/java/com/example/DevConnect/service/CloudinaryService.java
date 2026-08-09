package com.example.DevConnect.service;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    public String uploadResume(MultipartFile file) {
        try {
            // Get original filename and its extension (e.g., .pdf)
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // Generate a unique filename using UUID to avoid collisions
            String uniquePublicId = UUID.randomUUID() + extension;

            Map<Object, Object> options = new HashMap<>();
            options.put("folder", "resumes");
            options.put("public_id", uniquePublicId);
            options.put("resource_type", "raw"); // Force storing as a raw document to preserve format

            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload resume to Cloudinary", e);
        }
    }
}
