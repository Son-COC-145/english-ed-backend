package com.example.english_app.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    @Pattern(
            regexp = "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$",
            message = "Email không hợp lệ"
    )
    private String email;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên từ 2-100 ký tự")
    private String fullName;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])"
                    + "(?=.*[@#$%^&+=!])"
                    + "(?=\\S+$)"
                    + ".{8,72}$",
            message = "Mật khẩu phải từ 8-72 ký tự, không có khoảng trắng, "
                    + "bao gồm ít nhất 1 chữ hoa, 1 chữ thường, "
                    + "1 số và 1 ký tự đặc biệt"
    )
    private String password;

    @Pattern(
            regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$",
            message = "Số điện thoại không hợp lệ"
    )
    private String phone;
}



