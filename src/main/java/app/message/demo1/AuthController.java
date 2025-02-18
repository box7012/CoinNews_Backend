package app.message.demo1;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = { "https://coin-dashboard.xyz", "http:192.168.0.2:8080" })
public class AuthController {

    private final Log log = LogFactory.getLog(AuthController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public String register(@RequestBody Map<String, String> user) {
        User newUser = new User();
        newUser.setEmail(user.get("email"));
        newUser.setPassword(passwordEncoder.encode(user.get("password")));
        userRepository.save(newUser);
        return "User registered successfully";
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> user) {
        // 이메일로 사용자 검색
        User existingUser = userRepository.findByEmail(user.get("email"))
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 비밀번호 검증
        if (passwordEncoder.matches(user.get("password"), existingUser.getPassword())) {
            // JWT 토큰 생성
            String token = jwtUtil.generateToken(existingUser.getEmail());

            log.info("로그인 성공!");
            log.info("Generated Token: " + token);

            // 성공적인 로그인 시, ResponseEntity로 JWT 반환
            return ResponseEntity.ok(token);  // 200 OK 응답과 함께 토큰 반환
        } else {
            // 인증 실패 시 401 Unauthorized 응답 반환
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid credentials");  // 401 응답과 함께 메시지 반환
        }
    }

}