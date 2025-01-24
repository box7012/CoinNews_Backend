package app.message.demo1;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;


@CrossOrigin
@Controller
@RequestMapping("/api")
public class SignupController {

    @Autowired
    private UserRepository userRepository;

    private final static Log log = LogFactory.getLog(SignupController.class);

    @GetMapping("/{path:^(?!api).*$}")
    public String forward() {
        return "forward:/index.html";
    }

    @PostMapping("/emailcheck")
    public ResponseEntity<String> emailCheck(@Valid @RequestBody SignupDuplicateCheckDTO signupDuplicateCheckDTO) {
        log.info("Received email check request for: " + signupDuplicateCheckDTO.getEmail());
        try {
            if (userRepository.existsByEmail(signupDuplicateCheckDTO.getEmail())) {
                log.info("Email already exists: " + signupDuplicateCheckDTO.getEmail());
                return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 존재하는 이메일 입니다.");
            } else {
                log.info("Email is valid: " + signupDuplicateCheckDTO.getEmail());
                return ResponseEntity.ok("유효한 이메일 입니다.");
            }
        } catch (Exception e) {
            log.error("Error occurred during email check: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("중복 확인 실패");
        }
    }

    
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequest signupRequest) {
        try {
            if (userRepository.existsByEmail(signupRequest.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 존재하는 이메일 입니다.");
            } else {
                User user = new User();
                user.setEmail(signupRequest.getEmail());
                user.setPassword(signupRequest.getPassword());
                user.setCreatedAt(new Date());
                userRepository.save(user);
                return ResponseEntity.ok("회원가입 성공");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("회원가입 실패");
        }
    }    
}
