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
import java.util.Map;

@Service
public class BackTester {

       private final Log log = LogFactory.getLog(AuthController.class);

       private final SparkSession spark;
       
       @Autowired
       public BackTester(SparkSession spark) {
              this.spark = spark;
       }

//     public Dataset<Row> backTestingRSIDataset(List<OHLCData> dataset, Map<String, String> condition) {
//        // OHLCData 리스트를 DataFrame으로 변환
       
//        Dataset<Row> df = spark.createDataFrame(dataset, OHLCData.class);
//        Double buyCriterion = Double.parseDouble(condition.get("buy"));
//        Double sellCriterion = Double.parseDouble(condition.get("sell"));
    
//        // 전체 데이터에 대해 timestamp 기준 오름차순 정렬
//        df = df.orderBy("timestamp");
    
//        // 전일 대비 가격 변화 계산
//        df = df.withColumn("change", 
//                col("tradePrice").minus(lag("tradePrice", 1).over(Window.orderBy("timestamp"))));
    
//        // 상승(gain)과 하락(loss) 분리
//        df = df.withColumn("gain", when(col("change").gt(0), col("change")).otherwise(lit(0)))
//               .withColumn("loss", when(col("change").lt(0), col("change").multiply(-1)).otherwise(lit(0)));
    
//        // 최근 14일의 데이터에 대해 평균 상승과 평균 하락 계산
//        WindowSpec windowSpec = Window.orderBy("timestamp").rowsBetween(-13, 0);
//        df = df.withColumn("avgGain", avg(col("gain")).over(windowSpec))
//               .withColumn("avgLoss", avg(col("loss")).over(windowSpec));
    
//        // RS (Relative Strength) 계산
//        df = df.withColumn("rs", col("avgGain").divide(col("avgLoss")));
    
//        // RSI 계산
//        df = df.withColumn("rsi", 
//                when(col("avgLoss").equalTo(0), lit(100))
//                .otherwise(lit(100).minus(lit(100).divide(col("rs").plus(1)))));
    
//        // RSI가 0 또는 100인 데이터 필터링
//        df = df.filter(col("rsi").notEqual(0).and(col("rsi").notEqual(100)));
    
//        // 매수 및 매도 신호 설정
//        df = df.withColumn("buySignal", when(col("rsi").leq(buyCriterion), col("tradePrice")).otherwise(lit(null)))
//               .withColumn("sellSignal", when(col("rsi").geq(sellCriterion), col("tradePrice")).otherwise(lit(null)));
    
//        return df;
//     }

       public Dataset<Row> backTestingRSIDataset(List<OHLCData> dataset, Map<String, String> condition) {
              Dataset<Row> df = spark.createDataFrame(dataset, OHLCData.class);
              Double buyCriterion = Double.parseDouble(condition.get("buy"));
              Double sellCriterion = Double.parseDouble(condition.get("sell"));
       
              df = df.orderBy("timestamp");
       
              df = df.withColumn("change",
                     col("tradePrice").minus(lag("tradePrice", 1).over(Window.orderBy("timestamp"))));
       
              df = df.withColumn("gain", when(col("change").gt(0), col("change")).otherwise(lit(0)))
                     .withColumn("loss", when(col("change").lt(0), col("change").multiply(-1)).otherwise(lit(0)));
       
              WindowSpec windowSpec = Window.orderBy("timestamp").rowsBetween(-13, 0);
              df = df.withColumn("avgGain", avg(col("gain")).over(windowSpec))
                     .withColumn("avgLoss", avg(col("loss")).over(windowSpec));
       
              df = df.withColumn("rs", col("avgGain").divide(col("avgLoss")));
       
              df = df.withColumn("rsi",
                     when(col("avgLoss").equalTo(0), lit(100))
                            .otherwise(lit(100).minus(lit(100).divide(col("rs").plus(1)))));
       
              df = df.filter(col("rsi").notEqual(0).and(col("rsi").notEqual(100)));
       
              // 이전 RSI 값 계산
              df = df.withColumn("prev_rsi", lag("rsi", 1).over(Window.orderBy("timestamp")));
       
              // 기존 buySignal, sellSignal과 AND 연산 수행
              df = df.withColumn("buySignal",
                     col("buySignal").and(  // 기존 buySignal이 true일 때만 새로운 조건 적용
                            col("prev_rsi").leq(buyCriterion).and(col("rsi").gt(buyCriterion))
                     ));
       
              df = df.withColumn("sellSignal",
                     col("sellSignal").and(  // 기존 sellSignal이 true일 때만 새로운 조건 적용
                            col("prev_rsi").geq(sellCriterion).and(col("rsi").lt(sellCriterion))
                     ));
       
              return df;
       }

       public Dataset<Row> backTestingCCIDataset(List<OHLCData> dataset, Map<String, String> condition) {

              Dataset<Row> df = spark.createDataFrame(dataset, OHLCData.class);
              Double buyCriterion = Double.parseDouble(condition.get("buy"));
              Double sellCriterion = Double.parseDouble(condition.get("sell"));
          
              df = df.orderBy("timestamp");
          
              // TP (Typical Price) 계산
              df = df.withColumn("tp", col("high").plus(col("low")).plus(col("tradePrice")).divide(3));
          
              // 최근 20일간 이동 평균 & 평균 절대 편차 계산
              WindowSpec windowSpec = Window.orderBy("timestamp").rowsBetween(-19, 0);
              df = df.withColumn("sma_tp", avg(col("tp")).over(windowSpec));
          
              df = df.withColumn("mad_tp", avg(abs(col("tp").minus(col("sma_tp")))).over(windowSpec));
          
              // CCI 계산
              df = df.withColumn("cci", col("tp").minus(col("sma_tp"))
                      .divide(col("mad_tp").multiply(0.015)));
          
              // 이전 CCI 값
              df = df.withColumn("prev_cci", lag("cci", 1).over(Window.orderBy("timestamp")));
          
              // 기존 buySignal, sellSignal과 AND 연산 수행
              df = df.withColumn("buySignal",
                      col("buySignal").and(  // 기존 buySignal이 true일 때만 새로운 조건 적용
                              col("prev_cci").leq(buyCriterion).and(col("cci").gt(buyCriterion))
                      ));
          
              df = df.withColumn("sellSignal",
                      col("sellSignal").and(  // 기존 sellSignal이 true일 때만 새로운 조건 적용
                              col("prev_cci").geq(sellCriterion).and(col("cci").lt(sellCriterion))
                      ));
          
              return df;
       }
          

       public Dataset<Row> backTestingPSYDataset(List<OHLCData> dataset, Map<String, String> condition) {

              Dataset<Row> df = spark.createDataFrame(dataset, OHLCData.class);
              Double buyCriterion = Double.parseDouble(condition.get("buy"));
              Double sellCriterion = Double.parseDouble(condition.get("sell"));
          
              df = df.orderBy("timestamp");
          
              // 전일 종가와 비교하여 상승 여부 확인 (상승일 = 1, 하락일 = 0)
              df = df.withColumn("upDay", when(col("tradePrice").gt(lag("tradePrice", 1).over(Window.orderBy("timestamp"))), lit(1)).otherwise(lit(0)));
          
              // 최근 12일 동안의 상승일 수 계산
              WindowSpec windowSpec = Window.orderBy("timestamp").rowsBetween(-11, 0);
              df = df.withColumn("psy", avg(col("upDay")).over(windowSpec).multiply(100));
          
              // 이전 PSY 값
              df = df.withColumn("prev_psy", lag("psy", 1).over(Window.orderBy("timestamp")));
          
              // 기존 buySignal, sellSignal과 AND 연산 수행
              df = df.withColumn("buySignal",
                      col("buySignal").and(  // 기존 buySignal이 true일 때만 새로운 조건 적용
                              col("prev_psy").leq(buyCriterion).and(col("psy").gt(buyCriterion))
                      ));
          
              df = df.withColumn("sellSignal",
                      col("sellSignal").and(  // 기존 sellSignal이 true일 때만 새로운 조건 적용
                              col("prev_psy").geq(sellCriterion).and(col("psy").lt(sellCriterion))
                      ));
          
              return df;
       }
          

       public Dataset<Row> backTestingBollingerDataset(List<OHLCData> dataset, Map<String, String> condition) {
              Dataset<Row> df = spark.createDataFrame(dataset, OHLCData.class);
          
              df = df.orderBy("timestamp");
          
              // 최근 20일 이동 평균 및 표준 편차 계산
              WindowSpec windowSpec = Window.orderBy("timestamp").rowsBetween(-19, 0);
          
              df = df.withColumn("sma20", avg(col("tradePrice")).over(windowSpec))
                     .withColumn("stddev20", stddev(col("tradePrice")).over(windowSpec));
          
              // 볼린저 밴드 계산
              df = df.withColumn("upperBand", col("sma20").plus(col("stddev20").multiply(2)))
                     .withColumn("lowerBand", col("sma20").minus(col("stddev20").multiply(2)));
          
              // 이전 종가 및 볼린저 밴드 값
              df = df.withColumn("prev_tradePrice", lag("tradePrice", 1).over(Window.orderBy("timestamp")));
              df = df.withColumn("prev_lowerBand", lag("lowerBand", 1).over(Window.orderBy("timestamp")));
              df = df.withColumn("prev_upperBand", lag("upperBand", 1).over(Window.orderBy("timestamp")));
          
              // 기존 buySignal, sellSignal과 AND 연산 수행
              df = df.withColumn("buySignal",
                      col("buySignal").and(  // 기존 buySignal이 true일 때만 새로운 조건 적용
                              col("prev_tradePrice").lt(col("prev_lowerBand"))
                                      .and(col("tradePrice").gt(col("lowerBand")))
                      ));
          
              df = df.withColumn("sellSignal",
                      col("sellSignal").and(  // 기존 sellSignal이 true일 때만 새로운 조건 적용
                              col("prev_tradePrice").gt(col("prev_upperBand"))
                                      .and(col("tradePrice").lt(col("upperBand")))
                      ));
          
              return df;
       }
          

       public Dataset<Row> backTestingMACDDataset(List<OHLCData> dataset, Map<String, String> condition) {
              Dataset<Row> df = spark.createDataFrame(dataset, OHLCData.class);
          
              df = df.orderBy("timestamp");
          
              // 지수 이동 평균(EMA) 계산을 위한 Window 정의
              WindowSpec window12 = Window.orderBy("timestamp").rowsBetween(-11, 0);
              WindowSpec window26 = Window.orderBy("timestamp").rowsBetween(-25, 0);
              WindowSpec window9 = Window.orderBy("timestamp").rowsBetween(-8, 0);
          
              // EMA 계산 (단순히 AVG를 사용했지만, 실제로는 지수 이동 평균이 필요)
              df = df.withColumn("ema12", avg(col("tradePrice")).over(window12));
              df = df.withColumn("ema26", avg(col("tradePrice")).over(window26));
          
              // MACD 라인 계산
              df = df.withColumn("macd", col("ema12").minus(col("ema26")));
          
              // Signal 라인 계산
              df = df.withColumn("signal", avg(col("macd")).over(window9));
          
              // 이전 MACD와 Signal 값 계산
              df = df.withColumn("prev_macd", lag("macd", 1).over(Window.orderBy("timestamp")));
              df = df.withColumn("prev_signal", lag("signal", 1).over(Window.orderBy("timestamp")));
          
              // 기존 buySignal, sellSignal과 AND 연산 수행
              df = df.withColumn("buySignal",
                      col("buySignal").and(  // 기존 buySignal이 true일 때만 새로운 조건 적용
                              col("prev_macd").leq(col("prev_signal")).and(col("macd").gt(col("signal")))
                      ));
          
              df = df.withColumn("sellSignal",
                      col("sellSignal").and(  // 기존 sellSignal이 true일 때만 새로운 조건 적용
                              col("prev_macd").geq(col("prev_signal")).and(col("macd").lt(col("signal")))
                      ));
          
              return df;
       }
          
}
