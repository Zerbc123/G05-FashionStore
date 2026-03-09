package vn.edu.fpt.fashionstore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendEmail(String email , String fullName , String username , String password,  String roleName) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("Thông báo tạo tài khoản nhân viên - FashionStore");
        msg.setText(
                "Xin chào " + fullName + ",\n\n" +
                        "Tài khoản nhân viên của bạn trên hệ thống FashionStore đã được tạo thành công.\n\n" +
                        "Thông tin đăng nhập của bạn như sau:\n" +
                        "----------------------------------------\n" +
                        "Họ và tên: " + fullName + "\n" +
                        "Username: " + username + "\n" +
                        "Mật khẩu: " + password + "\n" +
                        "Vai trò: " + roleName  + "\n" +
                        "----------------------------------------\n\n" +
                        "Trân trọng,\n" +
                        "FashionStore Admin"
        );
        mailSender.send(msg);
    }
}
