package com.example.atp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AtpRequest {
    private String customerId;
    private OrderType orderType;
    private List<AtpRequestItem> items;
}