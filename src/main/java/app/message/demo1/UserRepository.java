package app.message.demo1;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;


import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
    private final DataSource dataSource;

    public UserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void save(User user) {
        String sql = "INSERT INTO users (email, password, created_at) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getPassword());
            // stmt.setDate(3, new java.sql.Date(user.getCreatedAt().getTime()));
            stmt.setDate(3, new java.sql.Date(System.currentTimeMillis())); // 현재 시간
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, email, password, created_at FROM users WHERE email = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                User user = new User();
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setCreatedAt(rs.getDate("created_at"));
                return Optional.of(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}


// import java.util.Optional;

// import org.apache.commons.logging.Log;
// import org.apache.commons.logging.LogFactory;
// import org.hibernate.Session;
// import org.hibernate.SessionFactory;
// import org.hibernate.query.Query;
// import org.springframework.stereotype.Repository;

// import jakarta.transaction.Transactional;

// @Repository
// public class UserRepository {

//     private SessionFactory sessionFactory;

//     private final static Log log = LogFactory.getLog(UserRepository.class);

//     public UserRepository(SessionFactory sessionFactory) {
//         this.sessionFactory = sessionFactory;
//     }

    
//     /**
//      * 이메일 중복 여부 확인
//      *
//      * @param email 확인할 이메일
//      * @return 중복 여부 (true: 중복, false: 중복 아님)
//      */
//     public boolean existsByEmail(String email) {
//         try (Session session = sessionFactory.openSession()) {
//             String hql = "SELECT COUNT(u) FROM User u WHERE u.email = :email";
//             Query<Long> query = session.createQuery(hql, Long.class);
//             query.setParameter("email", email);
//             Long count = query.uniqueResult();
//             return count > 0;
//         } catch (Exception e) {
//             log.error("Error while checking email existence: " + email, e);
//             return false;
//         }
//     }

//     /**
//      * 사용자 저장
//      *
//      * @param user 저장할 사용자 객체
//      */
//     public void save(User user) {
//         try (Session session = sessionFactory.openSession()) {
//             session.beginTransaction();
//             session.save(user);
//             session.getTransaction().commit();
//             log.info("User saved successfully: " + user.getEmail());
//         } catch (Exception e) {
//             log.error("Error while saving user: " + user.getEmail(), e);
//             throw e; // 예외를 상위로 전달하여 컨트롤러에서 처리
//         }
//     }

//     /**
//      * 이메일로 사용자 검색
//      *
//      * @param email 검색할 이메일
//      * @return Optional<User> 검색된 사용자 객체
//      */
    
//     @Transactional
//     public Optional<User> findByEmail(String email) {
//         try (Session session = sessionFactory.openSession()) {
//             String hql = "FROM User u WHERE u.email = :email";
//             Query<User> query = session.createQuery(hql, User.class);
//             query.setParameter("email", email);
//             User user = query.uniqueResult();
//             return Optional.ofNullable(user);
//         } catch (Exception e) {
//             log.error("Error while finding user by email: " + email, e);
//             return Optional.empty();
//         }
//     }


// }