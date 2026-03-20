package vn.edu.fpt.fashionstore.service;

import vn.edu.fpt.fashionstore.entity.SupportChat;
import vn.edu.fpt.fashionstore.entity.SenderType;

import java.time.LocalDateTime;
import java.util.List;

public interface SupportChatService {

    // Lấy tất cả tin nhắn của một support request
    List<SupportChat> getMessagesBySupportRequestId(Long supportRequestId);

    // Gửi tin nhắn mới
    SupportChat sendMessage(Long supportRequestId, SenderType senderType, String senderName, 
                           String messageContent, Integer staffId);

    // Gửi tin nhắn từ customer
    SupportChat sendCustomerMessage(Long supportRequestId, String customerName, String messageContent);

    // Gửi tin nhắn từ staff
    SupportChat sendStaffMessage(Long supportRequestId, Integer staffId, String staffName, String messageContent);

    // Đánh dấu tin nhắn đã đọc
    void markMessagesAsRead(Long supportRequestId, SenderType senderType);

    // Đánh dấu tin nhắn của customer đã đọc (khi staff xem)
    void markCustomerMessagesAsRead(Long supportRequestId);

    // Đánh dấu tin nhắn của staff đã đọc (khi customer xem)
    void markStaffMessagesAsRead(Long supportRequestId);

    // Lấy tin nhắn chưa đọc của staff
    List<SupportChat> getUnreadMessagesForStaff(Integer staffId);

    // Đếm số tin nhắn chưa đọc của staff
    long countUnreadMessagesForStaff(Integer staffId);

    // Đếm số tin nhắn chưa đọc của customer cho một support request
    long countUnreadCustomerMessages(Long supportRequestId);

    // Lấy lịch sử tin nhắn của staff
    List<SupportChat> getStaffMessageHistory(Integer staffId);

    // Lấy lịch sử tin nhắn của staff trong khoảng thời gian
    List<SupportChat> getStaffMessageHistoryByDateRange(Integer staffId, LocalDateTime startDate, LocalDateTime endDate);

    // Lấy tin nhắn gần đây của staff
    List<SupportChat> getRecentMessagesForStaff(Integer staffId, int limit);

    // Xóa tất cả tin nhắn của một support request
    void deleteMessagesBySupportRequestId(Long supportRequestId);
}
