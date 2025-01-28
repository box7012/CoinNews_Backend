package app.message.demo1;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwt;

@Controller
@RequestMapping("/api")
public class CommunityBoardController {

    @Autowired
    private CommunityBoardService communityBoardService;

    @PostMapping("/posts")
    @ResponseBody
    public ResponseEntity<String> createPost(@RequestBody CommunityBoardPost newPost) {
        String email = getUserEmailFromToken();

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        newPost.setEmail(email);
        communityBoardService.savePost(newPost);

        return ResponseEntity.ok("게시글 저장 완료!");
    }

    // JWT에서 이메일 추출하는 메소드
    private String getUserEmailFromToken() {
        try {
            Jws<Claims> jws = (Jws<Claims>) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return jws.getBody().get("sub", String.class);
        } catch (Exception e) {
            return null;
        }
    }

}
