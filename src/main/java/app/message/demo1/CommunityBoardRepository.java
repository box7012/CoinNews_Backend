package app.message.demo1;

import java.lang.StackWalker.Option;
import java.util.List;
import java.util.Optional;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;



@Repository
public class CommunityBoardRepository {

    private SessionFactory sessionFactory;

    public CommunityBoardRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    } 
    
    public void save(Post post) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.save(post);
            session.getTransaction().commit();
        } catch (Exception e) {
            throw e;
        }
    }

    public List<Post> findAll() {
        Session session = sessionFactory.openSession();

        try {
            String hql = "FROM Post";
            Query<Post> query = session.createQuery(hql, Post.class);
            return query.list();
        } finally {
            session.close();
        }

    }

    public Optional<Post> findById(Long postId) {
        try {
            Session session = sessionFactory.openSession();
            String hql = "FROM Post u WHERE u.id = :postId";
            Query<Post> query = session.createQuery(hql, Post.class);
            query.setParameter("id", postId);
            Post post = query.uniqueResult();
            return Optional.ofNullable(post);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void deleteById(Long postId) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            String hql = "DELETE FROM Post WHERE id = :postId";
            Query query = session.createNamedQuery(hql);
            query.setParameter("postId", postId);
            int result = query.executeUpdate();
            transaction.commit();

            if (result == 0) {
                throw new Error("삭제할 게시글을 찾을 수 없습니다. ID: " + postId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("게시글 삭제 중 문제가 발생했습니다.", e);
        }
    }


}
