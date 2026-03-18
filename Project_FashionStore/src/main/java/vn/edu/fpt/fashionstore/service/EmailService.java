package vn.edu.fpt.fashionstore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Gửi email khi tạo tài khoản nhân viên
    public void sendEmail(String email, String fullName, String username, String password, String roleName) {

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("Thông báo tạo tài khoản nhân viên - FashionStore");

        msg.setText(
                "Xin chào " + fullName + ",\n\n" +
                "Tài khoản nhân viên của bạn trên hệ thống FashionStore đã được tạo thành công.\n\n" +
                "Thông tin đăng nhập:\n" +
                "----------------------------------------\n" +
                "Họ và tên: " + fullName + "\n" +
                "Username: " + username + "\n" +
                "Mật khẩu: " + password + "\n" +
                "Vai trò: " + roleName + "\n" +
                "----------------------------------------\n\n" +
                "Trân trọng,\n" +
                "FashionStore Admin"
        );

        mailSender.send(msg);
    }

    // Gửi OTP đăng ký
    public void sendOtpEmail(String toEmail, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Mã xác nhận đăng ký Fashion Store");

        message.setText(
                "Mã OTP của bạn là: " + otp +
                "\nMã có hiệu lực trong 5 phút."
        );

        mailSender.send(message);
    }

    // Gửi OTP khi quên mật khẩu
    public void sendForgotPasswordOtp(String toEmail, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Mã xác nhận đặt lại mật khẩu - Fashion Store");

        message.setText(
                "Mã OTP đặt lại mật khẩu của bạn là: " + otp +
                "\nMã có hiệu lực trong 5 phút."
        );

        mailSender.send(message);
    }
}