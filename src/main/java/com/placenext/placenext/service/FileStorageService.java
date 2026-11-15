package com.placenext.placenext.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final long MAX_RESUME_BYTES = 2 * 1024 * 1024; // 2 MB

    private final Path resumeDir;

    public FileStorageService(@Value("${placenext.upload-dir:uploads}") String uploadDir) throws IOException {
        this.resumeDir = Paths.get(uploadDir, "resumes").toAbsolutePath().normalize();
        Files.createDirectories(resumeDir);
    }

    public String storeResume(MultipartFile file) throws IOException {
        validatePdf(file);
        String storedName = UUID.randomUUID() + ".pdf";
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, resumeDir.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        }
        return storedName;
    }

    public Resource loadResume(String storedName) throws MalformedURLException {
        Path path = resumeDir.resolve(storedName).normalize();
        if (!path.startsWith(resumeDir)) {
            throw new IllegalArgumentException("Invalid file path");
        }
        return new UrlResource(path.toUri());
    }

    public void deleteResume(String storedName) {
        try {
            Files.deleteIfExists(resumeDir.resolve(storedName).normalize());
        } catch (IOException ignored) {
            // not critical: an orphaned file is harmless
        }
    }

    private void validatePdf(MultipartFile file) throws IOException {
        if (file.getSize() > MAX_RESUME_BYTES) {
            throw new IllegalArgumentException("Resume must be 2 MB or smaller.");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".pdf")) {
            throw new IllegalArgumentException("Resume must be a PDF file.");
        }
        try (InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(4);
            boolean isPdf = header.length == 4
                    && header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F';
            if (!isPdf) {
                throw new IllegalArgumentException("That file isn't a valid PDF.");
            }
        }
    }
}