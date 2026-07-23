package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, productRepository);
    }

    private Product visibleProduct(String id, String name, int price) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(price);
        product.setVisible(true);
        product.setImages(List.of(new ProductImage("img-1", "http://example.com/" + id + ".png", 0)));
        return product;
    }

    @Test
    void addItemMergesQuantityOnDuplicateAdd() {
        Cart cart = new Cart("cart-1", "user-1", new java.util.ArrayList<>(), Instant.now());
        cart.getItems().add(new CartItem("product-1", 2));

        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("product-1")).thenReturn(Optional.of(visibleProduct("product-1", "Glasses", 500)));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart updated = cartService.addItem("user-1", "product-1", 3);

        assertThat(updated.getItems()).hasSize(1);
        assertThat(updated.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void addItemRejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> cartService.addItem("user-1", "product-1", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cartService.addItem("user-1", "product-1", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addItemRejectsANonExistentProduct() {
        when(productRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem("user-1", "ghost", 1))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void addItemRejectsANonVisibleProduct() {
        Product hidden = visibleProduct("product-1", "Glasses", 500);
        hidden.setVisible(false);
        when(productRepository.findById("product-1")).thenReturn(Optional.of(hidden));

        assertThatThrownBy(() -> cartService.addItem("user-1", "product-1", 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateQuantityToZeroRemovesTheLine() {
        Cart cart = new Cart("cart-1", "user-1", new java.util.ArrayList<>(), Instant.now());
        cart.getItems().add(new CartItem("product-1", 4));

        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart updated = cartService.updateItemQuantity("user-1", "product-1", 0);

        assertThat(updated.getItems()).isEmpty();
    }

    @Test
    void updateQuantityToNegativeAlsoRemovesTheLine() {
        Cart cart = new Cart("cart-1", "user-1", new java.util.ArrayList<>(), Instant.now());
        cart.getItems().add(new CartItem("product-1", 4));

        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart updated = cartService.updateItemQuantity("user-1", "product-1", -3);

        assertThat(updated.getItems()).isEmpty();
    }

    @Test
    void totalsAreComputedCorrectlyInCents() {
        Cart cart = new Cart("cart-1", "user-1", new java.util.ArrayList<>(), Instant.now());
        cart.getItems().add(new CartItem("product-1", 2));
        cart.getItems().add(new CartItem("product-2", 3));

        when(productRepository.findById("product-1")).thenReturn(Optional.of(visibleProduct("product-1", "Glasses", 1099)));
        when(productRepository.findById("product-2")).thenReturn(Optional.of(visibleProduct("product-2", "Case", 250)));

        CartResponse response = cartService.toResponse(cart);

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getLineTotal()).isEqualTo(2198);
        assertThat(response.getItems().get(1).getLineTotal()).isEqualTo(750);
        assertThat(response.getTotal()).isEqualTo(2948);
    }

    @Test
    void aUserCannotSeeOrModifyAnotherUsersCart() {
        Cart cartForUserA = new Cart("cart-a", "user-a", new java.util.ArrayList<>(), Instant.now());
        cartForUserA.getItems().add(new CartItem("product-1", 1));

        when(cartRepository.findByUserId("user-b")).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart cartForUserB = cartService.getOrCreateCart("user-b");

        assertThat(cartForUserB.getUserId()).isEqualTo("user-b");
        assertThat(cartForUserB.getItems()).isEmpty();
        assertThat(cartForUserB).isNotSameAs(cartForUserA);

        // Removing an item for user-b must never touch user-a's stored cart.
        cartService.removeItem("user-b", "product-1");
        verify(cartRepository, never()).findByUserId("user-a");
        assertThat(cartForUserA.getItems()).hasSize(1);
    }
}
