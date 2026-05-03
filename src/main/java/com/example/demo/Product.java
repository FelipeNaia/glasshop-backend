package com.example.demo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product {

    @Id
    private String id;
    private String name;
    private int stockQuantity;
    private int price;
    private int amountSold;
    private List<ProductImage> images = new ArrayList<>();
    private List<String> tags = new ArrayList<>();
    private boolean visible = true;
}
