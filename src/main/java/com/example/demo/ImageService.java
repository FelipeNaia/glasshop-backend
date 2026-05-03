package com.example.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ProductRepository productRepository;
    private final ImageStorageService storageService;

    public Product uploadImage(String productId, MultipartFile file) throws IOException {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));

        String imageId = UUID.randomUUID().toString();
        storageService.store(imageId, file);

        List<ProductImage> images = product.getImages();
        int nextPriority = images.isEmpty() ? 0 :
            images.stream().mapToInt(ProductImage::getPriority).max().getAsInt() + 1;

        images.add(new ProductImage(imageId, "/api/images/" + imageId, nextPriority));
        return productRepository.save(product);
    }

    public Product reorderImages(String productId, List<String> orderedIds) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));

        // LinkedHashMap preserves insertion order for remaining images
        Map<String, ProductImage> imageMap = product.getImages().stream()
            .collect(Collectors.toMap(ProductImage::getId, img -> img, (a, b) -> a, LinkedHashMap::new));

        List<ProductImage> reordered = new ArrayList<>();
        for (int i = 0; i < orderedIds.size(); i++) {
            ProductImage img = imageMap.remove(orderedIds.get(i));
            if (img != null) {
                img.setPriority(i);
                reordered.add(img);
            }
        }
        // Append images not included in the reorder request, preserving their relative order
        int tail = reordered.size();
        for (ProductImage remaining : imageMap.values()) {
            remaining.setPriority(tail++);
            reordered.add(remaining);
        }

        product.setImages(reordered);
        return productRepository.save(product);
    }

    public ResponseEntity<Resource> getImage(String imageId) throws IOException {
        Resource resource = storageService.load(imageId);
        String contentType = storageService.detectContentType(imageId);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .body(resource);
    }
}
