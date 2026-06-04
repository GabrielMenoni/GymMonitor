package com.example.presenceservice.controller;

import com.example.presenceservice.dto.PresenceCountResponse;
import com.example.presenceservice.dto.PresenceHistoryResponse;
import com.example.presenceservice.dto.PresenceUsersResponse;
import com.example.presenceservice.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    @GetMapping("/count")
    public PresenceCountResponse count() {
        return presenceService.getCount();
    }

    @GetMapping("/users")
    public PresenceUsersResponse users() {
        return presenceService.getUsers();
    }

    @GetMapping("/history")
    public PresenceHistoryResponse history(
            @RequestParam long from,
            @RequestParam long to) {
        return presenceService.getHistory(from, to);
    }
}
