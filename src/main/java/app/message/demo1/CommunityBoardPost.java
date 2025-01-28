package app.message.demo1;

public class CommunityBoardPost {
    private String title;
    private String content;
    private String email;  // 이메일 필드, JWT에서 가져온 사용자 이메일을 설정
    
    // 기본 생성자
    public CommunityBoardPost() {}

    // Getter 및 Setter 메소드들
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}