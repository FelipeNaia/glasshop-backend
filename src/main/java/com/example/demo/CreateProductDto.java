package com.example.demo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductDto {
    private String name;
    private int stockQuantity;
    private int price;
    private int amountSold;
    private List<String> tags = new ArrayList<>();
    private boolean visible = true;
}
