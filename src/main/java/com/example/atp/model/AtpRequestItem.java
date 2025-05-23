package com.example.atp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AtpRequestItem {
    private String productId;
    private int requestedQuantity;
}