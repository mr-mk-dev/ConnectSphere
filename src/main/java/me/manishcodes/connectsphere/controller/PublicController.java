package me.manishcodes.connectsphere.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello World";
    }
}
