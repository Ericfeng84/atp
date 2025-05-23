package com.example.atp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AtpResponse {
    private String orderId;
    private List<AtpResultItem> results;
    private String overallStatus;
}