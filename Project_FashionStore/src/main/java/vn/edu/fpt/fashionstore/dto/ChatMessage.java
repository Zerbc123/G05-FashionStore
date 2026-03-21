package vn.edu.fpt.fashionstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private Long id;
    private Long supportRequestId;
    private String senderType; // "CUSTOMER" or "STAFF"
    private String senderName;
    private String messageContent;
    private LocalDateTime sentAt;
    private Boolean isRead;
    private Integer staffId;
    
    // For WebSocket messages
    private String type; // "MESSAGE", "TYPING", "READ_STATUS"
    private Long timestamp;
}
