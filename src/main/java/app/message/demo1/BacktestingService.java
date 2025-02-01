// package app.message.demo1;

// import java.util.Arrays;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;
// import org.apache.spark.sql.*;
// import org.apache.spark.sql.expressions.Window;

// import java.awt.image.BufferedImage;
// import java.io.ByteArrayOutputStream;
// import java.util.Base64;
// import javax.imageio.ImageIO;
// import java.util.Random;
// import org.knowm.xchart.*;

// @Service
// public class BacktestingService {

//     private final SparkSession spark;
    
//     @Autowired
//     public BacktestingService(SparkSession spark) {
//         this.spark = spark;
//     }

//     public String runMonteCarloSimulation(String jsonData) {
//         // ✅ JSON 데이터 로드 (ticker: "AAPL", close: 170.0 형식)
//         Dataset<Row> df = spark.read().json(spark.createDataset(Arrays.asList(jsonData), Encoders.STRING()));

//         // ✅ 필요한 컬럼 선택 (티커별 종가)
//         df = df.select("ticker", "close");

//         // ✅ 수익률 계산 (pct_change 대체)
//         Dataset<Row> shiftedDf = df.withColumn("prev_close", functions.lag("close", 1).over(
//                 Window.partitionBy("ticker").orderBy("close")
//         ));
//         Dataset<Row> dailyReturns = shiftedDf.withColumn("daily_ret",
//                 functions.expr("(close - prev_close) / prev_close"));

//         // ✅ 연평균 수익률 및 공분산 계산
//         Dataset<Row> meanReturn = dailyReturns.groupBy("ticker").agg(functions.avg("daily_ret").alias("annual_ret"));
//         Dataset<Row> covMatrix = dailyReturns.agg(functions.expr("cov(daily_ret, daily_ret)").alias("annual_cov"));
//         // ✅ 몬테카를로 시뮬레이션 실행
//         int simulations = 20000;
//         Random rand = new Random();
//         double[] returns = new double[simulations];
//         double[] risks = new double[simulations];

//         for (int i = 0; i < simulations; i++) {
//             double weight = rand.nextDouble();
//             double portfolioReturn = weight * meanReturn.first().getDouble(1);
//             double portfolioRisk = Math.sqrt(weight * weight * covMatrix.first().getDouble(1));

//             returns[i] = portfolioReturn;
//             risks[i] = portfolioRisk;
//         }

//         // ✅ 그래프 생성
//         BufferedImage chartImage = generateChart(returns, risks);

//         // ✅ Base64 인코딩하여 반환
//         return encodeImageToBase64(chartImage);
//     }

//     // 📌 XChart를 이용해 그래프 생성
//     private BufferedImage generateChart(double[] returns, double[] risks) {
//         XYChart chart = new XYChartBuilder().width(800).height(600).title("Monte Carlo Simulation")
//                 .xAxisTitle("Risk").yAxisTitle("Expected Return").build();

//         chart.addSeries("Simulations", risks, returns);
//         return BitmapEncoder.getBufferedImage(chart);
//     }

//     // 📌 Base64 인코딩
//     private String encodeImageToBase64(BufferedImage image) {
//         try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
//             ImageIO.write(image, "png", outputStream);
//             byte[] imageBytes = outputStream.toByteArray();
//             return Base64.getEncoder().encodeToString(imageBytes);
//         } catch (Exception e) {
//             throw new RuntimeException("이미지 인코딩 실패", e);
//         }
//     }
// }