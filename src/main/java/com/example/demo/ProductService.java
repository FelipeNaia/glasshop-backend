package com.example.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final MongoTemplate mongoTemplate;

    public Page<Product> getProducts(int page, int size, List<String> tags, Boolean visible) {
        Query countQuery = new Query();
        Query pageQuery = new Query();

        if (tags != null && !tags.isEmpty()) {
            countQuery.addCriteria(Criteria.where("tags").all(tags));
            pageQuery.addCriteria(Criteria.where("tags").all(tags));
        }
        if (visible != null) {
            countQuery.addCriteria(Criteria.where("visible").is(visible));
            pageQuery.addCriteria(Criteria.where("visible").is(visible));
        }

        long total = mongoTemplate.count(countQuery, Product.class);
        pageQuery.with(PageRequest.of(page, size));
        List<Product> products = mongoTemplate.find(pageQuery, Product.class);

        products.forEach(p -> p.getImages().sort(Comparator.comparingInt(ProductImage::getPriority)));

        return new PageImpl<>(products, PageRequest.of(page, size), total);
    }

    public Product toggleVisibility(String id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Product not found: " + id));
        product.setVisible(!product.isVisible());
        return productRepository.save(product);
    }
}
