package vn.edu.fpt.fashionstore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import vn.edu.fpt.fashionstore.dto.ChatMessage;
import vn.edu.fpt.fashionstore.entity.SupportChat;
import vn.edu.fpt.fashionstore.service.SupportChatService;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final SupportChatService supportChatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    // Handle incoming chat messages
    @MessageMapping("/chat/{supportRequestId}")
    @SendTo("/topic/chat/{supportRequestId}")
    public ChatMessage handleChatMessage(@DestinationVariable Long supportRequestId, ChatMessage message) {
        log.info("Received WebSocket message for support request {}: {}", supportRequestId, message.getMessageContent());
        
        try {
            // Save message to database
            SupportChat savedMessage;
            if ("STAFF".equals(message.getSenderType())) {
                savedMessage = supportChatService.sendStaffMessage(
                    supportRequestId, 
                    message.getStaffId(), 
                    message.getSenderName(), 
                    message.getMessageContent()
                );
            } else {
                savedMessage = supportChatService.sendCustomerMessage(
                    supportRequestId, 
                    message.getSenderName(), 
                    message.getMessageContent()
                );
            }

            // Convert to ChatMessage DTO
            ChatMessage response = new ChatMessage();
            response.setId(savedMessage.getId());
            response.setSupportRequestId(savedMessage.getSupportRequestId());
            response.setSenderType(savedMessage.getSenderType().toString());
            response.setSenderName(savedMessage.getSenderName());
            response.setMessageContent(savedMessage.getMessageContent());
            response.setSentAt(savedMessage.getSentAt());
            response.setIsRead(savedMessage.getIsRead());
            response.setStaffId(savedMessage.getStaffId());
            response.setType("MESSAGE");
            response.setTimestamp(System.currentTimeMillis());

            log.info("Broadcasting message to topic /topic/chat/{}", supportRequestId);
            return response;

        } catch (Exception e) {
            log.error("Error processing chat message: {}", e.getMessage(), e);
            
            // Send error message
            ChatMessage errorMessage = new ChatMessage();
            errorMessage.setType("ERROR");
            errorMessage.setMessageContent("Failed to send message: " + e.getMessage());
            errorMessage.setTimestamp(System.currentTimeMillis());
            
            return errorMessage;
        }
    }

    // Broadcast message to specific support request room
    public void broadcastToRoom(Long supportRequestId, SupportChat message) {
        try {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setId(message.getId());
            chatMessage.setSupportRequestId(message.getSupportRequestId());
            chatMessage.setSenderType(message.getSenderType().toString());
            chatMessage.setSenderName(message.getSenderName());
            chatMessage.setMessageContent(message.getMessageContent());
            chatMessage.setSentAt(message.getSentAt());
            chatMessage.setIsRead(message.getIsRead());
            chatMessage.setStaffId(message.getStaffId());
            chatMessage.setType("MESSAGE");
            chatMessage.setTimestamp(System.currentTimeMillis());

            String destination = "/topic/chat/" + supportRequestId;
            messagingTemplate.convertAndSend(destination, chatMessage);
            
            log.info("Broadcasted message to {}: {}", destination, message.getMessageContent());
        } catch (Exception e) {
            log.error("Error broadcasting message: {}", e.getMessage(), e);
        }
    }

    // Send typing indicator
    public void broadcastTypingIndicator(Long supportRequestId, String senderName, boolean isTyping) {
        try {
            ChatMessage typingMessage = new ChatMessage();
            typingMessage.setSupportRequestId(supportRequestId);
            typingMessage.setSenderName(senderName);
            typingMessage.setType("TYPING");
            typingMessage.setTimestamp(System.currentTimeMillis());
            
            // Use message content to indicate typing status
            typingMessage.setMessageContent(isTyping ? "true" : "false");

            String destination = "/topic/chat/" + supportRequestId;
            messagingTemplate.convertAndSend(destination, typingMessage);
            
            log.info("Broadcast typing indicator to {}: {} is typing: {}", 
                    destination, senderName, isTyping);
        } catch (Exception e) {
            log.error("Error broadcasting typing indicator: {}", e.getMessage(), e);
        }
    }

    // Send read status
    public void broadcastReadStatus(Long supportRequestId, String senderType) {
        try {
            ChatMessage readStatusMessage = new ChatMessage();
            readStatusMessage.setSupportRequestId(supportRequestId);
            readStatusMessage.setSenderType(senderType);
            readStatusMessage.setType("READ_STATUS");
            readStatusMessage.setMessageContent("READ");
            readStatusMessage.setTimestamp(System.currentTimeMillis());

            String destination = "/topic/chat/" + supportRequestId;
            messagingTemplate.convertAndSend(destination, readStatusMessage);
            
            log.info("Broadcast read status to {}: {}", destination, senderType);
        } catch (Exception e) {
            log.error("Error broadcasting read status: {}", e.getMessage(), e);
        }
    }
}
