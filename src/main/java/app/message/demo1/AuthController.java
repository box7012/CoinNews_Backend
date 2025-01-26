package app.message.demo1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public String register(@RequestBody Map<String, String> user) {
        User newUser = new User();
        newUser.setEmail(user.get("username"));
        newUser.setPassword(passwordEncoder.encode(user.get("password")));
        userRepository.save(newUser);
        return "User registered successfully";
    }

    @PostMapping("/login")
    public String login(@RequestBody Map<String, String> user) {
        // 이메일로 사용자 검색
        User existingUser = userRepository.findByEmail(user.get("email"))
                .orElseThrow(() -> new RuntimeException("User not found"));
    
        // 비밀번호 검증
        if (passwordEncoder.matches(user.get("password"), existingUser.getPassword())) {
            // JWT 토큰 생성 및 반환
            return jwtUtil.generateToken(existingUser.getEmail());
        } else {
            throw new RuntimeException("Invalid credentials");
        }
    }
}