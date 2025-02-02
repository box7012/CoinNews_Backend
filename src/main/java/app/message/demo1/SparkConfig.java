package app.message.demo1;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.apache.spark.sql.SparkSession;

@Configuration
public class SparkConfig {

    @Bean
    public SparkSession sparkSession() {
        System.setProperty("jdk.module.illegalAccess", "permit"); // 추가
        return SparkSession.builder()
                .appName("PortfolioOptimization")
                .master("local[*]")
                .config("spark.sql.shuffle.partitions", "4")
                .config("spark.hadoop.fs.defaultFS", "file:///")  // 로컬 파일 시스템 설정
                .getOrCreate();
    }
}