package vn.edu.fpt.fashionstore.dto;
import jakarta.validation.constraints.Size;

public class CreateAccountRequest {
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;
}
