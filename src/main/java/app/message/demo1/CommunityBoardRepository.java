package app.message.demo1;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

@Repository
public class CommunityBoardRepository {

    private DataSource dataSource;

    public CommunityBoardRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    } 
    
    // 게시글 저장
    public void save(Post post) {
        String sql = "INSERT INTO posts (email, title, text, created_date) VALUES (?, ?, ?)";  // 테이블에 맞게 수정
        
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
                post.setId(rs.getLong("id"));
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

    // 게시글 ID로 조회
    public Optional<Post> findById(Long postId) {
        String sql = "SELECT id, title, text, created_date FROM posts WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, postId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Post post = new Post();
                    post.setId(rs.getLong("id"));
                    post.setTitle(rs.getString("title"));
                    post.setText(rs.getString("text"));
                    post.setCreatedDate(rs.getTimestamp("created_date").toLocalDateTime());
                    return Optional.of(post);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    // 게시글 ID로 삭제
    public void deleteById(Long postId) {
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
}



// import java.util.List;
// import java.util.Optional;

// import org.hibernate.Session;
// import org.hibernate.SessionFactory;
// import org.hibernate.Transaction;
// import org.hibernate.query.Query;
// import org.springframework.stereotype.Repository;



// @Repository
// public class CommunityBoardRepository {

//     private SessionFactory sessionFactory;

//     public CommunityBoardRepository(SessionFactory sessionFactory) {
//         this.sessionFactory = sessionFactory;
//     } 
    
//     public void save(Post post) {
//         try (Session session = sessionFactory.openSession()) {
//             session.beginTransaction();
//             session.save(post);
//             session.getTransaction().commit();
//         } catch (Exception e) {
//             throw e;
//         }
//     }

//     public List<Post> findAll() {
//         Session session = sessionFactory.openSession();

//         try {
//             String hql = "FROM Post";
//             Query<Post> query = session.createQuery(hql, Post.class);
//             return query.list();
//         } finally {
//             session.close();
//         }

//     }

//     public Optional<Post> findById(Long postId) {
//         try {
//             Session session = sessionFactory.openSession();
//             String hql = "FROM Post u WHERE u.id = :postId";
//             Query<Post> query = session.createQuery(hql, Post.class);
//             query.setParameter("id", postId);
//             Post post = query.uniqueResult();
//             return Optional.ofNullable(post);
//         } catch (Exception e) {
//             return Optional.empty();
//         }
//     }

//     public void deleteById(Long postId) {
//         try (Session session = sessionFactory.openSession()) {
//             Transaction transaction = session.beginTransaction();
//             String hql = "DELETE FROM Post WHERE id = :postId";
//             Query query = session.createNamedQuery(hql);
//             query.setParameter("postId", postId);
//             int result = query.executeUpdate();
//             transaction.commit();

//             if (result == 0) {
//                 throw new Error("삭제할 게시글을 찾을 수 없습니다. ID: " + postId);
//             }
//         } catch (Exception e) {
//             e.printStackTrace();
//             throw new RuntimeException("게시글 삭제 중 문제가 발생했습니다.", e);
//         }
//     }


// }
