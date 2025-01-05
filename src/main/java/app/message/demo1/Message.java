package app.message.demo1;

import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "text", nullable = false, length = 128)
    private String text;

    @Column(name = "created_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    // 기본 생성자 추가
    public Message() {
        // 하이버네이트가 엔티티를 생성할 때 사용
    }

    // 필요한 필드를 받는 생성자
    public Message(String text) {
        this.text = text;
        this.createdDate = new Date();  // 기본 생성일을 현재 시간으로 설정
    }

    public String getText() {
        return text;
    }

    public Integer getId() {
        return id;
    }

    public Date getCreatedDate() {
        return createdDate;
    }
}