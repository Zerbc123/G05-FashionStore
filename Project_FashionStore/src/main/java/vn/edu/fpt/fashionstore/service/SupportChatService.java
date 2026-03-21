package vn.edu.fpt.fashionstore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.fashionstore.entity.SupportChat;
import vn.edu.fpt.fashionstore.entity.SenderType;
import vn.edu.fpt.fashionstore.repository.SupportChatRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SupportChatService {

    @Autowired
    private SupportChatRepository supportChatRepository;

    public List<SupportChat> getMessagesBySupportRequestId(Long supportRequestId) {
        if (supportRequestId == null) {
            throw new IllegalArgumentException("Support Request ID cannot be null");
        }
        return supportChatRepository.findBySupportRequestIdOrderBySentAtAsc(supportRequestId);
    }

    @Transactional
    public SupportChat sendMessage(Long supportRequestId, SenderType senderType, String senderName, 
                                  String messageContent, Integer staffId) {
        if (supportRequestId == null) {
            throw new IllegalArgumentException("Support Request ID cannot be null");
        }
        if (senderType == null) {
            throw new IllegalArgumentException("Sender type cannot be null");
        }
        if (senderName == null || senderName.trim().isEmpty()) {
            throw new IllegalArgumentException("Sender name cannot be null or empty");
        }
        if (messageContent == null || messageContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be null or empty");
        }

        SupportChat chat = new SupportChat();
        chat.setSupportRequestId(supportRequestId);
        chat.setSenderType(senderType);
        chat.setSenderName(senderName.trim());
        chat.setMessageContent(messageContent.trim());
        chat.setStaffId(staffId);

        return supportChatRepository.save(chat);
    }

    @Transactional
    public SupportChat sendCustomerMessage(Long supportRequestId, String customerName, String messageContent) {
        return sendMessage(supportRequestId, SenderType.CUSTOMER, customerName, messageContent, null);
    }

    @Transactional
    public SupportChat sendStaffMessage(Long supportRequestId, Integer staffId, String staffName, String messageContent) {
        return sendMessage(supportRequestId, SenderType.STAFF, staffName, messageContent, staffId);
    }

    @Transactional
    public void markMessagesAsRead(Long supportRequestId, SenderType senderType) {
        if (supportRequestId == null) {
            throw new IllegalArgumentException("Support Request ID cannot be null");
        }
        if (senderType == null) {
            throw new IllegalArgumentException("Sender type cannot be null");
        }
        supportChatRepository.markMessagesAsRead(supportRequestId, senderType);
    }

    @Transactional
    public void markCustomerMessagesAsRead(Long supportRequestId) {
        markMessagesAsRead(supportRequestId, SenderType.CUSTOMER);
    }

    @Transactional
    public void markStaffMessagesAsRead(Long supportRequestId) {
        markMessagesAsRead(supportRequestId, SenderType.STAFF);
    }

    public List<SupportChat> getUnreadMessagesForStaff(Integer staffId) {
        if (staffId == null) {
            throw new IllegalArgumentException("Staff ID cannot be null");
        }
        return supportChatRepository.findBySupportRequestIdAndSenderTypeAndIsReadFalseOrderBySentAtAsc(
                null, SenderType.CUSTOMER);
    }

    public long countUnreadMessagesForStaff(Integer staffId) {
        if (staffId == null) {
            throw new IllegalArgumentException("Staff ID cannot be null");
        }
        return supportChatRepository.countUnreadMessagesByStaffId(staffId);
    }

    public long countUnreadCustomerMessages(Long supportRequestId) {
        if (supportRequestId == null) {
            throw new IllegalArgumentException("Support Request ID cannot be null");
        }
        return supportChatRepository.countUnreadCustomerMessagesBySupportRequestId(supportRequestId);
    }

    public List<SupportChat> getStaffMessageHistory(Integer staffId) {
        if (staffId == null) {
            throw new IllegalArgumentException("Staff ID cannot be null");
        }
        return supportChatRepository.findByStaffIdOrderBySentAtDesc(staffId);
    }

    public List<SupportChat> getStaffMessageHistoryByDateRange(Integer staffId, LocalDateTime startDate, LocalDateTime endDate) {
        if (staffId == null) {
            throw new IllegalArgumentException("Staff ID cannot be null");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("End date cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        return supportChatRepository.findByStaffIdAndSentAtBetweenOrderBySentAtDesc(staffId, startDate, endDate);
    }

    public List<SupportChat> getRecentMessagesForStaff(Integer staffId, int limit) {
        if (staffId == null) {
            throw new IllegalArgumentException("Staff ID cannot be null");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }
        
        // Repository chỉ có method findTop10, nên nếu limit > 10 thì lấy 10
        int actualLimit = Math.min(limit, 10);
        return supportChatRepository.findTop10ByStaffIdOrderBySentAtDesc(staffId);
    }

    @Transactional
    public void deleteMessagesBySupportRequestId(Long supportRequestId) {
        if (supportRequestId == null) {
            throw new IllegalArgumentException("Support Request ID cannot be null");
        }
        supportChatRepository.deleteBySupportRequestId(supportRequestId);
    }
}
