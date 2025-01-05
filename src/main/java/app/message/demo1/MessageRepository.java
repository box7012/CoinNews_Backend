package app.message.demo1;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class MessageRepository {

    private SessionFactory sessionFactory;

    private final static Log log = LogFactory.getLog(MessageRepository.class);

    public MessageRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
   

    public Message saveMessage(Message message) {
        Session session = sessionFactory.openSession();
        session.save(message);
        return message;
    }
    
    public boolean deleteMessage(Long id) {
        Session session = null;
        Transaction transaction = null;
        
        try {
            // 세션 시작
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();

            // 삭제할 메시지 객체 조회
            Message message = session.get(Message.class, id);
            if (message != null) {
                session.delete(message); // 메시지 삭제
                transaction.commit(); // 커밋
                return true; // 삭제 성공
            } else {
                return false; // 메시지가 없으면 삭제 실패
            }
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback(); // 오류 발생 시 롤백
            }
            e.printStackTrace();
            return false; // 예외 처리 후 실패
        } finally {
            if (session != null) {
                session.close(); // 세션 종료
            }
        }
    }

    // 모든 메시지 조회
    public List<Message> findAll() {
        Session session = sessionFactory.openSession();
        try {
            // HQL 쿼리를 사용하여 모든 메시지 조회
            String hql = "FROM Message"; // Message 테이블의 모든 레코드를 조회
            Query<Message> query = session.createQuery(hql, Message.class);
            return query.list(); // 메시지 목록 반환
        } finally {
            session.close(); // 세션 종료
        }
    }
}
