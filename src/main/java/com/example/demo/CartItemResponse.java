package com.example.demo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private String productId;
    private String name;
    private int price;
    private String image;
    private int quantity;
    private int lineTotal;
}
