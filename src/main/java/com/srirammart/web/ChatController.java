package com.srirammart.web;

import com.srirammart.config.AppUserDetails;
import com.srirammart.service.ChatService;
import com.srirammart.service.UserService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {
    public record ChatRequest(String message, Long productId, List<ChatService.Turn> history) {}

    private final ChatService chat;
    private final UserService users;

    public ChatController(ChatService chat, UserService users) {
        this.chat = chat;
        this.users = users;
    }

    @PostMapping("/api/chat")
    public ChatService.Reply chat(@RequestBody ChatRequest req, @AuthenticationPrincipal AppUserDetails me) {
        return chat.reply(users.get(me), req.message(), req.productId(), req.history());
    }
}
