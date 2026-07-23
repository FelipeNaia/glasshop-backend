package com.example.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public Cart getOrCreateCart(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(newCart(userId)));
    }

    public Cart addItem(String userId, String productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be a positive integer");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));
        if (!product.isVisible()) {
            throw new IllegalArgumentException("Product is not available: " + productId);
        }

        Cart cart = getOrCreateCart(userId);
        Optional<CartItem> existing = findLine(cart, productId);
        if (existing.isPresent()) {
            existing.get().setQuantity(existing.get().getQuantity() + quantity);
        } else {
            cart.getItems().add(new CartItem(productId, quantity));
        }
        cart.setUpdatedAt(Instant.now());
        return cartRepository.save(cart);
    }

    public Cart updateItemQuantity(String userId, String productId, int quantity) {
        Cart cart = getOrCreateCart(userId);
        if (quantity <= 0) {
            cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        } else {
            CartItem item = findLine(cart, productId)
                    .orElseThrow(() -> new NoSuchElementException("Item not in cart: " + productId));
            item.setQuantity(quantity);
        }
        cart.setUpdatedAt(Instant.now());
        return cartRepository.save(cart);
    }

    public Cart removeItem(String userId, String productId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        cart.setUpdatedAt(Instant.now());
        return cartRepository.save(cart);
    }

    public Cart clearCart(String userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cart.setUpdatedAt(Instant.now());
        return cartRepository.save(cart);
    }

    public CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::enrich)
                .collect(Collectors.toList());
        int total = items.stream().mapToInt(CartItemResponse::getLineTotal).sum();
        return new CartResponse(cart.getId(), cart.getUserId(), items, total, cart.getUpdatedAt());
    }

    private CartItemResponse enrich(CartItem item) {
        return productRepository.findById(item.getProductId())
                .map(product -> {
                    String image = product.getImages().stream()
                            .min(Comparator.comparingInt(ProductImage::getPriority))
                            .map(ProductImage::getUrl)
                            .orElse(null);
                    int lineTotal = product.getPrice() * item.getQuantity();
                    return new CartItemResponse(
                            item.getProductId(), product.getName(), product.getPrice(), image, item.getQuantity(), lineTotal);
                })
                .orElseGet(() -> new CartItemResponse(item.getProductId(), null, 0, null, item.getQuantity(), 0));
    }

    private Optional<CartItem> findLine(Cart cart, String productId) {
        return cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();
    }

    private Cart newCart(String userId) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setItems(new ArrayList<>());
        cart.setUpdatedAt(Instant.now());
        return cart;
    }
}
