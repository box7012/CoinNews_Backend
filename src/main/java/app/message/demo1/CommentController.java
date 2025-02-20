package app.message.demo1;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@CrossOrigin(origins = {"https://coin-dashboard.xyz", "http://192.168.0.2:8080", "http://localhost:5173"})
public class CommentController {

    @Autowired
    private CommentService commentService;

    private final Log log = LogFactory.getLog(BacktestingController.class);

    @PostMapping
    public ResponseEntity<Comment> createComment(@PathVariable Long postId, @RequestBody Comment commentRequest) {
        
        log.info(commentRequest);
        log.info("CommentController commentRequest");
        Comment comment = commentService.addComment(postId, commentRequest.getEmail(), commentRequest.getText());
        return ResponseEntity.ok(comment);
    }

    @GetMapping
    public ResponseEntity<List<Comment>> getComments(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getCommentsByPostId(postId));
    }
}