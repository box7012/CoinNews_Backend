package app.message.demo1;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;



@Repository
public class NewsRepository {

    private SessionFactory sessionFactory;

    private final static Log log = LogFactory.getLog(NewsRepository.class);

    public NewsRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public List<News> findAll() {
        Session session = sessionFactory.openSession();

        try {
            String hql = "FROM News";
            Query<News> query = session.createQuery(hql, News.class);
            return query.list();
        } finally {
            session.close();
        }

    }


    
}
