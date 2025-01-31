package app.message.demo1;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

@Controller("/api")
@CrossOrigin(origins = "http://192.168.0.2:8080")
public class BacktestingController {
    
}
