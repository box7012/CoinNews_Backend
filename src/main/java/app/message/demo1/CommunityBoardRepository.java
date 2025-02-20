package app.message.demo1;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

@Repository
public class CommunityBoardRepository {

    private DataSource dataSource;

    private final static Log log = LogFactory.getLog(CommunityBoardRepository.class);

    public CommunityBoardRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    } 
    
    // 게시글 저장
    public void save(Post post) {
        String sql = "INSERT INTO posts (email, title, text, created_date) VALUES (?, ?, ?, ?)";  // 테이블에 맞게 수정
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, post.getEmail());
            stmt.setString(2, post.getTitle());
            stmt.setString(3, post.getText());
            stmt.setTimestamp(4, Timestamp.valueOf(post.getCreatedDate()));  // 날짜를 Timestamp로 변환
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("게시글 저장 중 오류가 발생했습니다.", e);
        }
    }

    // 모든 게시글 조회
    public List<Post> findAll() {
        List<Post> postList = new ArrayList<>();
        String sql = "SELECT id, email, title, text, created_date FROM posts";  // 테이블에 맞게 수정

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Post post = new Post();
                post.setId(rs.getInt("id"));
                post.setEmail(rs.getString("email"));
                post.setTitle(rs.getString("title"));
                post.setText(rs.getString("text"));
                post.setCreatedDate(rs.getTimestamp("created_date").toLocalDateTime());
                postList.add(post);
            }
        } catch (SQLException e) {
            throw new RuntimeException("게시글 조회 중 오류가 발생했습니다.", e);
        }

        return postList;
    }

    // // 게시글 ID로 조회
    // public Optional<Post> findById(int postId) {
    //     String sql = "SELECT id, email, title, text, created_date, views FROM posts WHERE id = ?";

    //     try (Connection conn = dataSource.getConnection();
    //         PreparedStatement stmt = conn.prepareStatement(sql)) {
    //         stmt.setInt(1, postId);  // int 타입이므로 setInt() 사용

    //         try (ResultSet rs = stmt.executeQuery()) {
    //             if (rs.next()) {
    //                 Post post = new Post();
    //                 post.setId(rs.getInt("id"));
    //                 post.setEmail(rs.getString("email"));
    //                 post.setTitle(rs.getString("title"));
    //                 post.setText(rs.getString("text"));
    //                 post.setCreatedDate(rs.getTimestamp("created_date").toLocalDateTime());
    //                 post.setViews(rs.getInt("views")); // 조회수 컬럼 추가 처리
    //                 return Optional.of(post);
    //             }
    //         }
    //     } catch (SQLException e) {
    //         e.printStackTrace();
    //     }
    //     return Optional.empty();
    // }

    // 게시글 ID로 조회 및 조회수 1 증가
    public Optional<Post> findById(int postId) {
        String sql = "SELECT id, email, title, text, created_date, views FROM posts WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, postId);  // int 타입이므로 setInt() 사용

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Post post = new Post();
                    post.setId(rs.getInt("id"));
                    post.setEmail(rs.getString("email"));
                    post.setTitle(rs.getString("title"));
                    post.setText(rs.getString("text"));
                    post.setCreatedDate(rs.getTimestamp("created_date").toLocalDateTime());
                    post.setViews(rs.getInt("views")); // 조회수 컬럼 추가 처리

                    // 조회수 1 증가
                    int updatedViews = post.getViews() + 1;
                    post.setViews(updatedViews);

                    // 조회수 업데이트 쿼리 실행
                    String updateSql = "UPDATE posts SET views = ? WHERE id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, updatedViews);
                        updateStmt.setInt(2, postId);
                        updateStmt.executeUpdate();
                    }

                    return Optional.of(post);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    // 게시글 ID로 삭제
    public void deleteById(int postId) {
        String sql = "DELETE FROM posts WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, postId);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected == 0) {
                throw new RuntimeException("삭제할 게시글을 찾을 수 없습니다. ID: " + postId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("게시글 삭제 중 오류가 발생했습니다.", e);
        }
    }

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Post p SET p.views = p.views + 1 WHERE p.id = :id")
    void incrementViews(@Param("id") int id) {
        log.info("incrementViews");
    }
}
