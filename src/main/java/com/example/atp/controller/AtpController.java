// src/main/java/com/example/atp/controller/
package com.example.atp.controller;

import com.example.atp.model.AtpRequest;
import com.example.atp.model.AtpResponse;
import com.example.atp.service.AtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atp")
public class AtpController {

    @Autowired
    private AtpService atpService;

    @PostMapping("/check")
    public ResponseEntity<AtpResponse> checkAvailability(@RequestBody AtpRequest request) {
        AtpResponse response = atpService.checkAvailability(request);
        return ResponseEntity.ok(response);
    }
}
