package app.message.demo1;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.socket.CloseStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.CopyOnWriteArrayList;

public class ChatHandler extends TextWebSocketHandler {

    private final Log log = LogFactory.getLog(ChatHandler.class);

    private static final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 메시지 JSON 파싱
        String payload = message.getPayload();
        log.info(payload);
        log.info("payload");
        ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);
        
        log.info(chatMessage);
        log.info("chatMessage");
        
        // 모든 클라이언트로 전송
        String jsonMessage = objectMapper.writeValueAsString(chatMessage);
        log.info(jsonMessage);
        log.info("jsonMessage");
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(jsonMessage));
                log.info(jsonMessage);
                log.info("jsonMessage");
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

}