package app.message.demo1;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommunityBoardRepository postRepository;

    public Comment addComment(Long postId, String email, String text) {
        // Post post = postRepository.findById(postId)
        //         .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
        Comment comment = new Comment(postId, email, text);
        commentRepository.save(comment); // save는 void이므로 return은 하지 않음
        return comment; // 저장된 comment를 반환
    }

    public List<Comment> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostId(postId);
    }
}