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
   
       // 전체 데이터에 대해 timestamp 기준 오름차순 정렬 (필요시)
       df = df.orderBy("timestamp");
   
       // 전일 대비 가격 변화 계산: 오늘의 tradePrice - 전일의 tradePrice
       df = df.withColumn("change", 
               col("tradePrice").minus(lag("tradePrice", 1).over(Window.orderBy("timestamp"))));
   
       // 상승(gain)과 하락(loss) 분리
       df = df.withColumn("gain", when(col("change").gt(0), col("change")).otherwise(lit(0)))
              .withColumn("loss", when(col("change").lt(0), col("change").multiply(-1)).otherwise(lit(0)));
   
       // 최근 14일의 데이터에 대해 평균 상승과 평균 하락 계산 (현재 행 포함하여 과거 13행까지)
       WindowSpec windowSpec = Window.orderBy("timestamp").rowsBetween(-13, 0);
       df = df.withColumn("avgGain", avg(col("gain")).over(windowSpec))
              .withColumn("avgLoss", avg(col("loss")).over(windowSpec));
   
       // RS (Relative Strength) 계산: 평균 상승 / 평균 하락
       df = df.withColumn("rs", col("avgGain").divide(col("avgLoss")));
   
       // RSI 계산:
       // 만약 avgLoss가 0이면 RSI를 100으로, 그렇지 않으면 RSI = 100 - (100 / (1 + RS))
       df = df.withColumn("rsi", 
               when(col("avgLoss").equalTo(0), lit(100))
               .otherwise(lit(100).minus(lit(100).divide(col("rs").plus(1)))));
   
       // 매수 및 매도 신호 설정 (예시: RSI가 20 이하이면 매수, 80 이상이면 매도)
       df = df.withColumn("buySignal", when(col("rsi").leq(20), col("tradePrice")).otherwise(lit(null)))
              .withColumn("sellSignal", when(col("rsi").geq(80), col("tradePrice")).otherwise(lit(null)));
   
       return df;
   }
}
