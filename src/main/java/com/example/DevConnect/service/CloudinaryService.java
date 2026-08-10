package com.example.DevConnect.service;

import com.cloudinary.Cloudinary;
import com.example.DevConnect.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    @Value("${app.upload.allowed-resume-types}")
    private List<String> allowedContentTypes;

    public String uploadResume(MultipartFile file) {
        validate(file);

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
            Object secureUrl = uploadResult.get("secure_url");
            if (secureUrl == null) {
                throw new IllegalStateException("Cloudinary did not return a secure_url for the uploaded resume");
            }
            return secureUrl.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to upload resume to Cloudinary", e);
        }
    }

    /**
     * Anything reaching Cloudinary is stored as a raw file and served from a public URL, so the
     * type is checked here rather than trusting whatever the client posted.
     */
    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select a file to upload.");
        }

        String contentType = file.getContentType();
        if (contentType == null || allowedContentTypes.stream().noneMatch(allowed -> allowed.equalsIgnoreCase(contentType.trim()))) {
            throw new BadRequestException("Unsupported resume type. Allowed types: " + String.join(", ", allowedContentTypes));
        }
    }
}
