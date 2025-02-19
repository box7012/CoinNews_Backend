package app.message.demo1;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

@Repository
public class CommentRepository {

    private final DataSource dataSource;
    private final CommunityBoardRepository postRepository; // PostRepository 주입

    public CommentRepository(DataSource dataSource, CommunityBoardRepository postRepository) {
        this.dataSource = dataSource;
        this.postRepository = postRepository;
    }

    public List<Comment> findByPostId(Long postId) {
        String sql = "SELECT * FROM comments WHERE post_id = ?";
        List<Comment> comments = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, postId);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Comment comment = new Comment();
                comment.setId(resultSet.getLong("id"));
                
                // post_id 값을 이용해 Post 객체를 조회하고 설정
                Long postIdFromDB = resultSet.getLong("post_id");
                Optional<Post> postOptional = postRepository.findById(postIdFromDB);
                Post post = postOptional.orElse(null); // 찾지 못한 경우 null로 처리
                comment.setPost(post); // setPost는 Post 객체를 받습니다

                comment.setText(resultSet.getString("text"));
                comment.setCreatedAt(resultSet.getTimestamp("created_at"));
                comments.add(comment);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return comments;
    }

    public void save(Comment comment) {
        String sql = "INSERT INTO comments (post_id, text, created_at) VALUES (?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, comment.getPost().getId()); // Post의 ID를 저장
            preparedStatement.setString(2, comment.getText());
            preparedStatement.setTimestamp(3, comment.getCreatedAt());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
