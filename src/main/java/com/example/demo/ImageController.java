package com.example.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/products/{productId}")
    public ResponseEntity<Product> uploadImage(
            @PathVariable String productId,
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(imageService.uploadImage(productId, file));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/products/{productId}/reorder")
    public ResponseEntity<Product> reorderImages(
            @PathVariable String productId,
            @RequestBody ReorderImagesRequest request) {
        try {
            return ResponseEntity.ok(imageService.reorderImages(productId, request.getImageIds()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/products/{productId}/{imageId}")
    public ResponseEntity<Product> deleteImage(
            @PathVariable String productId,
            @PathVariable String imageId) {
        try {
            return ResponseEntity.ok(imageService.deleteImage(productId, imageId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{imageId}")
    public ResponseEntity<Resource> getImage(@PathVariable String imageId) {
        try {
            return imageService.getImage(imageId);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
