package com.example.atp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SourcingRule {
    private Region customerRegion;
    private OrderType orderType;
    private PartMarking partMarking;
    private List<String> preferredWarehouseIds;
}