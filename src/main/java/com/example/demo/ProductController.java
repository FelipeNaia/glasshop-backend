package com.example.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository repository;
    private final ProductService productService;

    @GetMapping
    public Page<Product> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) Boolean visible) {
        return productService.getProducts(page, size, tags, visible);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> create(@RequestBody Product product) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable String id, @RequestBody Product updated) {
        return repository.findById(id).map(existing -> {
            existing.setName(updated.getName());
            existing.setStockQuantity(updated.getStockQuantity());
            existing.setPrice(updated.getPrice());
            existing.setAmountSold(updated.getAmountSold());
            existing.setTags(updated.getTags());
            existing.setVisible(updated.isVisible());
            return ResponseEntity.ok(repository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/sell")
    public ResponseEntity<Product> sell(@PathVariable String id) {
        return repository.findById(id).map(product -> {
            if (product.getStockQuantity() <= 0) {
                return ResponseEntity.badRequest().<Product>build();
            }
            product.setStockQuantity(product.getStockQuantity() - 1);
            product.setAmountSold(product.getAmountSold() + 1);
            return ResponseEntity.ok(repository.save(product));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<Product> toggleVisibility(@PathVariable String id) {
        try {
            return ResponseEntity.ok(productService.toggleVisibility(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
