package app.message.demo1;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
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

    private final static Log log = LogFactory.getLog(CommunityBoardController.class);

    @Autowired
    private CommunityBoardService communityBoardService;

    @GetMapping("/posts")
    @ResponseBody
    public ResponseEntity<List<Post>> getPosts() {
        List<Post> posts = communityBoardService.getAllPosts();
        return ResponseEntity.ok(posts);
    }


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

    private String getUserEmailFromToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
    
        try {
            // authentication.getPrincipal()이 String인 경우, 그 자체가 이메일임
            if (authentication.getPrincipal() instanceof String) {
                return (String) authentication.getPrincipal();  // 이메일 반환
            }
        } catch (Exception e) {
            return null;
        }
    
        return null;
    }

}
