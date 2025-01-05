package app.message.demo1;

import org.springframework.stereotype.Service;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

@Service
public class MessageService {

    private MessageRepository repository;

    
    public MessageService (MessageRepository repository) {
        this.repository = repository;
    }

    public List<Message> getAllMessages() {
        return repository.findAll(); // 메시지 전체 목록 반환
    }

    public Message save(String text) {
        return repository.saveMessage(new Message(text));
    }
    
    public boolean delete(Long id) {
        return repository.deleteMessage(id);
    }


}
