package app.message.demo1;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/api")
@CrossOrigin(origins = {"https://coin-dashboard.xyz", "http://192.168.0.2:8080", "http://localhost:5173"})
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

    @GetMapping("/posts/{id}")
    @ResponseBody
    public ResponseEntity<Post> getPost(@PathVariable int id) {
        return communityBoardService.getPostById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable int id, @RequestParam String email, 
                                           @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null || !userDetails.getUsername().equals(email)) {
            log.info(userDetails);
            log.info("userDetails");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    
        communityBoardService.deletePost(id, email);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts")
    @ResponseBody
    public ResponseEntity<String> createPost(@RequestBody CommunityBoardPost newPost) {
        
        String email = getUserEmailFromToken();
        log.info(email);
        log.info("email");
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        newPost.setEmail(email);
        communityBoardService.savePost(newPost);

        return ResponseEntity.ok("게시글 저장 완료!");
    }

    private String getUserEmailFromToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info(authentication);
        log.info("getUserEmailFromToken authentication");
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
    
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUsername();
        }
        // 또는 UserDetails를 사용 중이라면:
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return null;
    }

}
