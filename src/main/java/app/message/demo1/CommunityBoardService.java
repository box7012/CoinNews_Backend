package app.message.demo1;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CommunityBoardService {

    private final Log log = LogFactory.getLog(CommunityBoardService.class);

    private final CommunityBoardRepository postRepository;  // 게시글을 저장하는 Repository

    @Autowired
    public CommunityBoardService(CommunityBoardRepository postRepository) {
        this.postRepository = postRepository;
    }

    // 게시글 저장 메소드
    public void savePost(CommunityBoardPost post) {
        postRepository.save(convertToEntity(post));  // Post 객체를 데이터베이스에 저장
    }

    private Post convertToEntity(CommunityBoardPost dto) {

        Post post = new Post();
        post.setTitle(dto.getTitle());
        post.setText(dto.getText());
        post.setEmail(dto.getEmail());
        post.setCreatedDate(LocalDateTime.now()); // DTO에 생성 시간 정보가 있다면 사용
        // 필요한 필드를 추가적으로 매핑
        return post;
    }

    // 게시글 조회 메소드 예시 (게시글 목록을 반환)
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    // 특정 게시글 조회 메소드 예시
    public Optional<Post> getPostById(Long postId) {
        return postRepository.findById(postId);
    }

    // 게시글 삭제 메소드 예시
    public void deletePost(Long postId) {
        postRepository.deleteById(postId);
    }
}