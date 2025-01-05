package app.message.demo1;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/api")
public class MessageController {


    @Autowired
    private MessageService messageService;

    private final static Log log = LogFactory.getLog(MessageController.class);

    @GetMapping("/messages")
    @ResponseBody
    public ResponseEntity<List<Message>> getAllMessages() {
        List<Message> messages = messageService.getAllMessages();
        if (messages == null || messages.isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>()); // 메시지가 없으면 빈 배열 반환
        }
        return ResponseEntity.ok(messages); // 메시지가 있으면 모두 반환
    }

    @PostMapping("/messages")
    @ResponseBody
    public ResponseEntity<Message> saveMessage(@RequestBody MessageData data) {
        Message saved = messageService.save(data.getText());
        if (saved == null) {
            return ResponseEntity.status(500).build();
        }
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/messages/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        boolean deleted = messageService.delete(id);
        if (!deleted) {
            return ResponseEntity.status(404).build(); // 메시지가 없는 경우
        }
        return ResponseEntity.ok().build(); // 성공적으로 삭제된 경우
    }    
}
