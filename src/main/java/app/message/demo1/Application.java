package app.message.demo1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "app.message.demo1")
@EntityScan(basePackages = "app.message.demo1")
public class Application {

    public static void main(String[] args) {

        SpringApplication.run(Application.class, args);
    }
    
}
