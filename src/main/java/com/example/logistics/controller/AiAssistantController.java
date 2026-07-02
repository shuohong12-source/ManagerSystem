package com.example.logistics.controller;

import com.example.logistics.service.AiAssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class AiAssistantController {
    private final AiAssistantService aiAssistantService;

    public AiAssistantController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @GetMapping("/ai")
    public String page() {
        return "ai-assistant";
    }

    @PostMapping("/api/ai/chat")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "");
        return ResponseEntity.ok(aiAssistantService.chat(message));
    }
}
