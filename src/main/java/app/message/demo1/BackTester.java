package app.message.demo1;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.spark.sql.*;
import org.apache.spark.sql.expressions.Window;
import org.apache.spark.sql.expressions.WindowSpec;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import static org.apache.spark.sql.functions.*;
import java.util.List;

@Service
public class BackTester {

    private final Log log = LogFactory.getLog(AuthController.class);

    private final SparkSession spark;
    
    @Autowired
    public BackTester(SparkSession spark) {
        this.spark = spark;
    }

    public Dataset<Row> backTestingRSIDataset(List<OHLCData> dataset) {
        // OHLCData 리스트를 DataFrame으로 변환
        Dataset<Row> df = spark.createDataFrame(dataset, OHLCData.class);
    
        // 윈도우 스펙: 최근 14개(0~13)
        WindowSpec windowSpec = Window.orderBy("timestamp").rowsBetween(-13, 0);
    
        log.info("안녕1");
    
        // 가격 변화 계산 (13일 전 tradePrice와 비교)
        df = df.withColumn("change", col("tradePrice").minus(lag("tradePrice", 13).over(Window.orderBy("timestamp"))));
        log.info("안녕1-2");
    
        // 상승과 하락을 분리
        df = df.withColumn("gain", when(col("change").gt(0), col("change")).otherwise(lit(0)))
               .withColumn("loss", when(col("change").lt(0), col("change").multiply(-1)).otherwise(lit(0)));
        log.info("안녕2");
    
        // 평균 상승 및 평균 하락 계산 (14개 데이터 평균)
        df = df.withColumn("avgGain", avg(col("gain")).over(windowSpec))
               .withColumn("avgLoss", avg(col("loss")).over(windowSpec));
        log.info("안녕3");
    
        // RS 및 RSI 계산
        df = df.withColumn("rs", col("avgGain").divide(col("avgLoss")))
               .withColumn("rsi", when(col("avgLoss").equalTo(0), lit(100))
                                  .otherwise(lit(100).minus(lit(100).divide(col("rs").plus(1)))));
        log.info("안녕4");
    
        // 매수 및 매도 신호 설정
        df = df.withColumn("buySignal", when(col("rsi").leq(20), col("tradePrice")).otherwise(lit(null)))
               .withColumn("sellSignal", when(col("rsi").geq(80), col("tradePrice")).otherwise(lit(null)));
    
        return df;
    }
}
