package me.manishcodes.connectsphere.controller;

import me.manishcodes.connectsphere.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicController {

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> hello() {
        return ResponseEntity.ok(ApiResponse.success("Public Endpoint Testing"));
    }

    @GetMapping("/protected/health")
    public ResponseEntity<ApiResponse<String>> protectHealth(){
        return  ResponseEntity.ok(ApiResponse.success("Protected Endpoint accessible"));
    }
}
