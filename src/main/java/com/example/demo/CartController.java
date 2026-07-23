package com.example.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public CartResponse getCart() {
        Cart cart = cartService.getOrCreateCart(currentUserId());
        return cartService.toResponse(cart);
    }

    @PostMapping("/items")
    public ResponseEntity<?> addItem(@RequestBody AddCartItemRequest request) {
        try {
            Cart cart = cartService.addItem(currentUserId(), request.getProductId(), request.getQuantity());
            return ResponseEntity.ok(cartService.toResponse(cart));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PatchMapping("/items/{productId}")
    public ResponseEntity<?> updateItemQuantity(
            @PathVariable String productId, @RequestBody UpdateCartItemRequest request) {
        try {
            Cart cart = cartService.updateItemQuantity(currentUserId(), productId, request.getQuantity());
            return ResponseEntity.ok(cartService.toResponse(cart));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(@PathVariable String productId) {
        Cart cart = cartService.removeItem(currentUserId(), productId);
        return cartService.toResponse(cart);
    }

    @DeleteMapping
    public CartResponse clearCart() {
        Cart cart = cartService.clearCart(currentUserId());
        return cartService.toResponse(cart);
    }

    private String currentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
