package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        JwtService.class
})
class ProductControllerSecurityTest {

    private static final String PRODUCT_JSON = "{\"name\":\"Frame\",\"price\":1000}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private ProductService productService;

    private String tokenFor(Role role) {
        User user = new User("user-1", "user@example.com", "hash", role, Instant.now());
        return jwtService.generateAccessToken(user);
    }

    @Test
    void buyerCannotCreateAProduct() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + tokenFor(Role.BUYER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateAProduct() throws Exception {
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + tokenFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isUnauthorized());
    }
}
