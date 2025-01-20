package app.message.demo1;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class SignupDuplicateCheckDTO {
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
}
