package com.example.atp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Warehouse {
    private String id;
    private String name;
    private Region region;
    private int processingLeadTimeDays;
}