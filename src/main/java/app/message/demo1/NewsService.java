package app.message.demo1;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class NewsService {

    private NewsRepository repository;

    public NewsService (NewsRepository repository) {
        this.repository = repository;
    }

    public List<News> getAllNews() {
        return repository.findAll();
    }
        
}
