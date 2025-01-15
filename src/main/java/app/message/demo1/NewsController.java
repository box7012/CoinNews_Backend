package app.message.demo1;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/api")
public class NewsController {

    @Autowired
    private NewsService newsService;

    private final static Log log = LogFactory.getLog(NewsController.class);


    @GetMapping("/news")
    @ResponseBody
    public ResponseEntity<List<News>> getAllNews() {
        List<News> news = newsService.getAllNews();
        if (news == null || news.isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }
        return ResponseEntity.ok(news);
    }

    @PostMapping("/news")
    @ResponseBody
    public ResponseEntity<List<News>> searchNews() {
        List<News> news = newsService.getAllNews();
        if (news == null || news.isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }
        return ResponseEntity.ok(news);
    }
}
