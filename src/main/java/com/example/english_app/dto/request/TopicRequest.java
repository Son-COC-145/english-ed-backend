package com.example.english_app.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicRequest {

    @NotBlank(message = "Không được để trống tên tiếng anh")
    @Size(max = 100, message = "Tên không được vượt quá 100 ký tự")
    private String nameEn;

    @NotBlank(message = "Không được để trống tên tiếng Việt")
    @Size(max = 100, message = "Tên không được vượt quá 100 ký tự")
    private String nameVi;

    private String iconUrl;
    private Boolean isActive;
}
