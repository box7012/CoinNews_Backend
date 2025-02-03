package app.message.demo1;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Repository;

@Repository
public class NewsRepository {

    private DataSource dataSource;

    private final static Log log = LogFactory.getLog(NewsRepository.class);

    public NewsRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<News> findAll() {
        List<News> newsList = new ArrayList<>();
        String sql = "SELECT id, title, link, date FROM crawled_news";  // 테이블 이름과 컬럼 이름 확인
    
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                News news = new News();
                news.setId(rs.getInt("id"));  // id 추가
                news.setTitle(rs.getString("title"));
                news.setLink(rs.getString("link"));
                news.setDate(rs.getTimestamp("date"));
                newsList.add(news);
            }
        } catch (SQLException e) {
            log.error("Error retrieving news", e);
        }
    
        // 확인을 위한 로그 추가
        log.info("Retrieved " + newsList.size() + " news articles.");
        
        return newsList;
    }
}



// import java.util.List;

// import org.apache.commons.logging.Log;
// import org.apache.commons.logging.LogFactory;
// import org.hibernate.Session;
// import org.hibernate.SessionFactory;
// import org.hibernate.query.Query;
// import org.springframework.stereotype.Repository;



// @Repository
// public class NewsRepository {

//     private SessionFactory sessionFactory;

//     private final static Log log = LogFactory.getLog(NewsRepository.class);

//     public NewsRepository(SessionFactory sessionFactory) {
//         this.sessionFactory = sessionFactory;
//     }

//     public List<News> findAll() {
//         Session session = sessionFactory.openSession();

//         try {
//             String hql = "FROM News";
//             Query<News> query = session.createQuery(hql, News.class);
//             return query.list();
//         } finally {
//             session.close();
//         }

//     }
// }
