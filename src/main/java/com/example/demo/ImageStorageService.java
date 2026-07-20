package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

@Service
public class ImageStorageService {

    private final Path uploadDir;

    public ImageStorageService(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadDir, e);
        }
    }

    public void store(String imageId, MultipartFile file) throws IOException {
        String original = StringUtils.cleanPath(
            file.getOriginalFilename() != null ? file.getOriginalFilename() : imageId
        );
        int dotIdx = original.lastIndexOf('.');
        String ext = dotIdx >= 0 ? original.substring(dotIdx) : "";
        Files.copy(file.getInputStream(), uploadDir.resolve(imageId + ext), StandardCopyOption.REPLACE_EXISTING);
    }

    public Resource load(String imageId) throws IOException {
        Path file = findFile(imageId);
        if (file == null) throw new FileNotFoundException("Image not found: " + imageId);
        Resource resource = new UrlResource(file.toUri());
        if (!resource.exists() || !resource.isReadable()) throw new FileNotFoundException("Image not readable: " + imageId);
        return resource;
    }

    public String detectContentType(String imageId) throws IOException {
        Path file = findFile(imageId);
        if (file == null) return "application/octet-stream";
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        String probed = Files.probeContentType(file);
        return probed != null ? probed : "application/octet-stream";
    }

    public void delete(String imageId) throws IOException {
        Path file = findFile(imageId);
        if (file != null) Files.deleteIfExists(file);
    }

    private Path findFile(String imageId) throws IOException {
        if (!Files.exists(uploadDir)) return null;
        try (Stream<Path> files = Files.list(uploadDir)) {
            return files
                .filter(p -> {
                    String n = p.getFileName().toString();
                    return n.equals(imageId) || n.startsWith(imageId + ".");
                })
                .findFirst()
                .orElse(null);
        }
    }
}
