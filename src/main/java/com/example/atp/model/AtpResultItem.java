package com.example.atp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AtpResultItem {
    private String originalProductId;
    private String fulfilledProductId;
    private int requestedQuantity;
    private int confirmedQuantity;
    private String sourceWarehouseId;
    private LocalDate estimatedShipDate;
    private String message;
}