package app.message.demo1;

import java.util.ArrayList;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.spark.sql.*;
import org.apache.spark.sql.expressions.Window;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

// import java.awt.List;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import java.util.Random;
import org.knowm.xchart.*;

@Service
public class BacktestingService {

    private final Log log = LogFactory.getLog(AuthController.class);

    private final SparkSession spark;
    
    @Autowired
    public BacktestingService(SparkSession spark) {
        this.spark = spark;
    }

    public String runMonteCarloSimulation(String jsonData) {
        // ✅ JSON 데이터 로드 (ticker: "AAPL", close: 170.0 형식)
        Dataset<Row> df = spark.read().json(spark.createDataset(Arrays.asList(jsonData), Encoders.STRING()));
    
        // ✅ 날짜별로 티커별 가격을 변환 (pivot)
        Dataset<Row> pivotedDf = df.groupBy("time")  // 날짜별로 그룹화
                                   .pivot("ticker")  // ticker별로 열을 만듬
                                   .agg(functions.first("tradePrice"));  // 가격을 첫 번째로 가져옴
    
        pivotedDf.show();
    
        // ✅ 각 티커에 대한 수익률 계산 (pct_change 대체)
        Dataset<Row> firstTicerShiftedDf = pivotedDf.withColumn("prev_ETH", functions.lag("ETH", 1).over(
                Window.orderBy("time")));
                firstTicerShiftedDf.show();

        Dataset<Row> secondTicerShiftedDf = firstTicerShiftedDf.withColumn("prev_BTC", functions.lag("BTC", 1).over(
            Window.orderBy("time")));
            secondTicerShiftedDf.show();
        
        Dataset<Row> firstdailyReturns = secondTicerShiftedDf.withColumn("daily_ret_ETH",
            functions.expr("(ETH - prev_ETH) / prev_ETH"));
            firstdailyReturns.show();

        Dataset<Row> seconddailyReturns = firstdailyReturns.withColumn("daily_ret_BTC",
            functions.expr("(BTC - prev_BTC) / prev_BTC"));
            seconddailyReturns.show();
                
        Dataset<Row> resultDf = seconddailyReturns.select(
            "daily_ret_ETH", "daily_ret_BTC"
        );
        
        resultDf.show();

        Dataset<Row> nullRemovedResultDf = resultDf.filter(resultDf.col("daily_ret_ETH").isNotNull());
        Dataset<Row> covCalculatedDataset = covCalculate(nullRemovedResultDf);
        covCalculatedDataset.show();

        log.info("여기까진 됐다! - 1");
    
        // ✅ 연평균 수익률 및 공분산 계산
        Dataset<Row> meanReturn = seconddailyReturns.groupBy("time").agg(functions.avg("daily_ret_ETH").alias("annual_ret_ETH"));
        
        // Covariance 행렬 계산
        Dataset<Row> covMatrixETH = resultDf.agg(functions.covar_samp("daily_ret_ETH", "daily_ret_ETH").alias("annual_cov_ETH"));
        covMatrixETH.show();
        log.info("여기까진 됐다! - 3");
        
        Dataset<Row> covMatrixBTC = resultDf.agg(functions.covar_samp("daily_ret_BTC", "daily_ret_BTC").alias("annual_cov_BTC"));
        log.info("여기까진 됐다! - 5");
        // daily_ret_ETH와 daily_ret_BTC의 평균값을 구하고 365배 하기
        Dataset<Row> avgReturns = seconddailyReturns.agg(
            (functions.avg("daily_ret_ETH").multiply(365)).alias("annual_ret_ETH"),
            (functions.avg("daily_ret_BTC").multiply(365)).alias("annual_ret_BTC")
        );
        avgReturns.show();
        log.info("여기까진 됐다! - 4");
        // ✅ 몬테카를로 시뮬레이션 실행
        int simulations = 20000;
        Random rand = new Random();

        Dataset<Row> null_removed_meanReturnETH = resultDf.filter(resultDf.col("daily_ret_ETH").isNotNull());
        log.info("calculatePortfolioMetrics");
        Map<String, List<?>> calculated_result = calculatePortfolioMetrics(avgReturns, covCalculatedDataset, 20000);

        // ✅ 그래프 생성
        // BufferedImage chartImage = generateChart(returns, risks);
        BufferedImage chartImage = generateChart(calculated_result.get("portRet"), calculated_result.get("portRisk"));
        
        // ✅ Base64 인코딩하여 반환
        return encodeImageToBase64(chartImage);
    }

    // 📌 XChart를 이용해 그래프 생성
    private BufferedImage generateChart(List returns, List risks) {
        XYChart chart = new XYChartBuilder().width(800).height(600).title("Monte Carlo Simulation")
                .xAxisTitle("Risk").yAxisTitle("Expected Return").build();

        chart.addSeries("Simulations", risks, returns);
        return BitmapEncoder.getBufferedImage(chart);
    }

    // 📌 Base64 인코딩
    private String encodeImageToBase64(BufferedImage image) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            throw new RuntimeException("이미지 인코딩 실패", e);
        }
    }

    public static Dataset<Row> covCalculate(Dataset<Row> df) {
        // time 컬럼 제외한 티커 리스트 추출
        List<String> tickers = new ArrayList<>(df.columns().length - 1);
        for (String col : df.columns()) {
            if (!col.equals("time")) {
                tickers.add(col);
            }
        }

        List<Row> covList = new ArrayList<>();
        for (String t1 : tickers) {
            for (String t2 : tickers) {
                double covValue = df.stat().cov(t1, t2);
                covList.add(RowFactory.create(t1, t2, covValue));
            }
        }

        // 스키마 정의
        StructType schema = DataTypes.createStructType(new StructField[]{
                DataTypes.createStructField("Ticker1", DataTypes.StringType, false),
                DataTypes.createStructField("Ticker2", DataTypes.StringType, false),
                DataTypes.createStructField("Covariance", DataTypes.DoubleType, false)
        });

        // Spark DataFrame 변환
        SparkSession spark = df.sparkSession();
        Dataset<Row> covDf = spark.createDataFrame(covList, schema);

        // 피벗하여 행렬 형태로 변환
        covDf = covDf.groupBy("Ticker1").pivot("Ticker2").agg(functions.first("Covariance"));

        // 최종 결과를 List<Row>로 변환하여 반환
        return covDf;
    }

    public Map<String, List<?>> calculatePortfolioMetrics(Dataset<Row> avgReturns, Dataset<Row> covCalculatedDataset, int simulations) {
        List<Double> portRet = new ArrayList<>();
        List<Double> portRisk = new ArrayList<>();
        List<double[]> portWeights = new ArrayList<>();
        List<Double> sharpeRatio = new ArrayList<>();

        // 평균 수익률 및 공분산 추출
        double[] annualRet = extractReturns(avgReturns);
        log.info("annualRet" + annualRet.length);

        double[][] annualCov = extractCovariance(covCalculatedDataset);

        // 시뮬레이션을 통한 포트폴리오 계산
        Random rand = new Random();

        for (int i = 0; i < simulations; i++) {
            // 랜덤 가중치 생성
            double[] weights = new double[annualCov.length];
            double weightSum = 0;
            for (int j = 0; j < weights.length; j++) {
                weights[j] = rand.nextDouble();
                weightSum += weights[j];
            }
            // 가중치 정규화
            for (int j = 0; j < weights.length; j++) {
                weights[j] /= weightSum;
            }

            // 포트폴리오 수익률 계산
            double returns = 0;
            for (int j = 0; j < annualRet.length; j++) {
                returns += weights[j] * annualRet[j];
            }

            // 포트폴리오 리스크 계산 (표준편차)
            RealMatrix covarianceMatrix = MatrixUtils.createRealMatrix(annualCov);
            RealVector weightVector = MatrixUtils.createRealVector(weights);
            RealVector weightedCovariance = covarianceMatrix.operate(weightVector);
            double risk = Math.sqrt(weightVector.dotProduct(weightedCovariance));

            // 샤프 비율 계산
            double sharpe = returns / risk;

            // 결과 저장
            portRet.add(returns);
            portRisk.add(risk);
            portWeights.add(weights);
            sharpeRatio.add(sharpe);
        }
        Map<String, List<?>> result = new HashMap<>();
        result.put("portRet", portRet);
        result.put("portRisk", portRisk);
        result.put("portWeights", portWeights);
        result.put("sharpeRatio", sharpeRatio);

        return result;
    }

    private double[] extractReturns(Dataset<Row> avgReturns) {
        // avgReturns에서 연간 수익률 추출 (가정: 각 row가 티커의 평균 수익률을 담고 있다고 가정)
        int numTickers = (int) avgReturns.count();
        double[] returns = new double[numTickers];

        for (int i = 0; i < numTickers; i++) {
            Row row = avgReturns.collectAsList().get(i);
            returns[i] = row.getDouble(1); // assuming the second column contains the return values
        }

        return returns;
    }

    private double[][] extractCovariance(Dataset<Row> covCalculatedDataset) {
        // covCalculatedDataset에서 연간 공분산 행렬 추출 (가정: 각 row가 티커들 간의 공분산을 담고 있다고 가정)
        int numTickers = (int) covCalculatedDataset.count();
        double[][] covarianceMatrix = new double[numTickers][numTickers];

        for (int i = 0; i < numTickers; i++) {
            Row row = covCalculatedDataset.collectAsList().get(i);
            for (int j = 0; j < numTickers; j++) {
                covarianceMatrix[i][j] = row.getDouble(j + 1); // assuming that columns 1 to N contain the covariance values
            }
        }

        return covarianceMatrix;
    }

}