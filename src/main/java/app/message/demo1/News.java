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
@Table(name = "crawled_news")
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id", nullable = false)
    private Integer id;

    @Column(name = "title", nullable = true, length = 256)
    private String title;

    @Column(name = "link", nullable = true, length = 256)
    private String link;


    @Column(name = "date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date date;

    public News() {
    }

    public String getLink() {
        return link;
    }

    public String getTitle() {
        return title;
    }

    public Integer getId() {
        return id;
    }   
    
    public Date getDate() {
        return date;
    }






    
}
