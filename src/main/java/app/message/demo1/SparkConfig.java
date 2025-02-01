package app.message.demo1;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.apache.spark.sql.SparkSession;

@Configuration
public class SparkConfig {

    @Bean
    public SparkSession sparkSession() {
        return SparkSession.builder()
                .appName("PortfolioOptimization")
                .master("local[*]")  // 로컬 실행 (클러스터에서는 변경)
                .config("spark.sql.shuffle.partitions", "4") // 설정 추가 가능
                .getOrCreate();
    }
}