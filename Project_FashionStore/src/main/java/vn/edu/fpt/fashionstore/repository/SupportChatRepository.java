package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.fashionstore.entity.SupportChat;
import vn.edu.fpt.fashionstore.entity.SenderType;

import java.time.LocalDateTime;
import java.util.List;

public interface SupportChatRepository extends JpaRepository<SupportChat, Long> {

    // Lấy tất cả tin nhắn của một support request theo thứ tự thời gian
    List<SupportChat> findBySupportRequestIdOrderBySentAtAsc(Long supportRequestId);

    // Lấy tin nhắn chưa đọc của customer cho một support request
    List<SupportChat> findBySupportRequestIdAndSenderTypeAndIsReadFalseOrderBySentAtAsc(
            Long supportRequestId, SenderType senderType);

    // Đánh dấu tất cả tin nhắn của một support request là đã đọc
    @Modifying
    @Query("UPDATE SupportChat sc SET sc.isRead = true WHERE sc.supportRequestId = :supportRequestId AND sc.senderType = :senderType")
    void markMessagesAsRead(@Param("supportRequestId") Long supportRequestId, @Param("senderType") SenderType senderType);

    // Lấy tin nhắn theo staff_id
    List<SupportChat> findByStaffIdOrderBySentAtDesc(Integer staffId);

    // Lấy tin nhắn theo staff_id và khoảng thời gian
    List<SupportChat> findByStaffIdAndSentAtBetweenOrderBySentAtDesc(
            Integer staffId, LocalDateTime startDateTime, LocalDateTime endDateTime);

    // Đếm số tin nhắn chưa đọc của staff
    @Query("SELECT COUNT(sc) FROM SupportChat sc WHERE sc.staffId = :staffId AND sc.senderType = 'CUSTOMER' AND sc.isRead = false")
    long countUnreadMessagesByStaffId(@Param("staffId") Integer staffId);

    // Đếm số tin nhắn chưa đọc của customer cho một support request
    @Query("SELECT COUNT(sc) FROM SupportChat sc WHERE sc.supportRequestId = :supportRequestId AND sc.senderType = 'STAFF' AND sc.isRead = false")
    long countUnreadCustomerMessagesBySupportRequestId(@Param("supportRequestId") Long supportRequestId);

    // Lấy tin nhắn gần đây của staff
    List<SupportChat> findTop10ByStaffIdOrderBySentAtDesc(Integer staffId);

    // Xóa tất cả tin nhắn của một support request
    void deleteBySupportRequestId(Long supportRequestId);
}
