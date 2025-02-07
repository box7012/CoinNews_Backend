package app.message.demo1;

import java.util.ArrayList;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.expression.Lists;

import shaded.parquet.it.unimi.dsi.fastutil.Hash;

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

import static org.apache.spark.sql.functions.log;

// import java.awt.List;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.sql.Date;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        // Step 1: Pivot the DataFrame
        Dataset<Row> pivotedDf = df.groupBy("time")  // Group by time
                                .pivot("ticker")  // Pivot by ticker
                                .agg(functions.first("tradePrice"));  // Use the first trade price for each ticker

        pivotedDf = pivotedDf.na().drop();

        // Step 2: Extract ticker columns (excluding "time")
        String[] tickerColumns = pivotedDf.columns();
        List<String> tickers = Arrays.stream(tickerColumns)
                                    .filter(col -> !col.equals("time"))
                                    .collect(Collectors.toList());

        // Step 3: Create "prev" columns using lag function
        Dataset<Row> shiftedDf = pivotedDf;
        for (String ticker : tickers) {
            String prevTicker = "prev_" + ticker;
            shiftedDf = shiftedDf.withColumn(prevTicker, functions.lag(ticker, 1).over(Window.orderBy("time")));
        }

        // Step 4: Calculate daily returns
        Dataset<Row> dailyReturnsDf = shiftedDf;
        for (String ticker : tickers) {
            String dailyRet = "daily_ret_" + ticker;
            String prevTicker = "prev_" + ticker;
            dailyReturnsDf = dailyReturnsDf.withColumn(dailyRet, functions.expr("(" + ticker + " - " + prevTicker + ") / " + prevTicker));
        }

        // Step 5: Select only daily return columns
        List<String> dailyRetColumns = tickers.stream()
                                            .map(ticker -> "daily_ret_" + ticker)
                                            .collect(Collectors.toList());

        Dataset<Row> resultDf = dailyReturnsDf.selectExpr(dailyRetColumns.toArray(new String[0]));

        // Step 6: Remove rows with null values in the first daily return column
        Dataset<Row> nullRemovedResultDf = resultDf.filter(resultDf.col(dailyRetColumns.get(0)).isNotNull());

    
        // 데이터프레임의 모든 컬럼 중에서 "daily_ret"이 포함된 컬럼명만 필터링
        String[] RetColumns = Arrays.stream(dailyReturnsDf.columns())
                .filter(colName -> colName.contains("daily_ret"))
                .toArray(String[]::new);

        // filteredDf는 daily_ret 컬럼만 포함하는 데이터프레임
        Dataset<Row> filteredDf = nullRemovedResultDf.select(
            Arrays.stream(RetColumns)
                .map(functions::col)  // String 배열을 Column 배열로 변환
                .toArray(Column[]::new)  // Column 배열로 변환
        );


        // Step 7: Calculate covariance (assuming covCalculate is a method defined elsewhere)
        Dataset<Row> covCalculatedDataset = covCalculate(filteredDf);

        // Step 8: Calculate annual returns
        Map<String, Column> aggregatedColumns = tickers.stream()
                .collect(Collectors.toMap(
                        ticker -> "annual_ret_" + ticker,  // Key: annual return column name
                        ticker -> functions.avg("daily_ret_" + ticker)  // Value: average of daily returns
                ));

        // aggregatedColumns에서 alias가 적용된 Column 배열 생성
        Column[] aggColumns = aggregatedColumns.entrySet().stream()
                .map(entry -> entry.getValue().alias(entry.getKey()))
                .toArray(Column[]::new);



        Dataset<Row> meanReturn = calculateAnnualReturn(nullRemovedResultDf);



        // log.info(" 여기까진왔다: " + tickers);
    
        String[] columns = filteredDf.columns();
        df.show();
        log.info(" 여기까진왔다1");

        pivotedDf.show();
        log.info(" 여기까진왔다2");

        // nullRemovedResultDf.show();
        // log.info(" 여기까진왔다3");    

        covCalculatedDataset.show();
        log.info(" 여기까진왔다4");    
         // ✅ 몬테카를로 시뮬레이션 실행
        Map<String, List<?>> calculated_result = calculatePortfolioMetrics(meanReturn, covCalculatedDataset, 20000);

        // ✅ 그래프 생성
        BufferedImage chartImage = generateChart(calculated_result.get("portRet"), calculated_result.get("portRisk"));
        
        // ✅ Base64 인코딩하여 반환
        String[] columnNames = pivotedDf.columns();
        for (String columnName : columnNames) {
            System.out.println(columnName);  // 컬럼명 출력
        }

        return encodeImageToBase64(chartImage);
    }

    // 📌 XChart를 이용해 그래프 생성
    private BufferedImage generateChart(List returns, List risks) {
        XYChart chart = new XYChartBuilder()
                            .width(800)
                            .height(600)
                            .title("Monte Carlo Simulation")
                            .xAxisTitle("Risk")
                            .yAxisTitle("Expected Return")
                            .build();
    
        // 시리즈 추가 및 Scatter 스타일로 설정
        XYSeries series = chart.addSeries("Simulations", risks, returns);
        series.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
        
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

    public Dataset<Row> covCalculate(Dataset<Row> df) {
        // time 컬럼 제외한 티커 리스트 추출

        List<String> tickers = new ArrayList<>();
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
    
        // 모든 수치 값(공분산 값)에 365를 곱함
        for (String ticker : tickers) {
            covDf = covDf.withColumn(ticker, functions.col(ticker).multiply(365));
        }
        
        return covDf;
    }


    public Map<String, List<?>> calculatePortfolioMetrics(Dataset<Row> avgReturns, Dataset<Row> covCalculatedDataset, int simulations) {
        List<Double> portRet = new ArrayList<>();
        List<Double> portRisk = new ArrayList<>();
        List<double[]> portWeights = new ArrayList<>();
        List<Double> sharpeRatio = new ArrayList<>();

        // 평균 수익률 및 공분산 추출
        double[] annualRet = extractReturns(avgReturns);
        avgReturns.show();
        log.info("avgReturns");

        log.info("annualRet: " + Arrays.toString(annualRet));
        log.info("annualRet length: " + annualRet.length);


        double[][] annualCov = extractCovariance(covCalculatedDataset);
        
        log.info("annualCov: " + Arrays.deepToString(annualCov));
        log.info("covcov");

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

        // 샤프 비율이 가장 높은 포트폴리오 찾기
        int maxSharpeIndex = IntStream.range(0, sharpeRatio.size())
                .boxed()
                .max(Comparator.comparing(sharpeRatio::get))
                .orElse(-1);

        if (maxSharpeIndex != -1) {
            double[] bestWeights = portWeights.get(maxSharpeIndex);
            log.info("최대 샤프 비율: " + sharpeRatio.get(maxSharpeIndex));
            log.info("최적 포트폴리오 가중치: " + Arrays.toString(bestWeights));
        }
        
        log.info(annualRet);
        return result;
    }

    // private double[] extractReturns(Dataset<Row> avgReturns) {
    //     // avgReturns에 한 행만 있다고 가정합니다.
    //     Row row = avgReturns.collectAsList().get(0);
    //     // 행에 포함된 컬럼 개수를 구합니다.
    //     int numCols = row.size();  // 또는 row.length()를 사용할 수 있습니다.
        
    //     double[] returns = new double[numCols];
    //     for (int i = 0; i < numCols; i++) {
    //         returns[i] = row.getDouble(i);
    //     }
        
    //     return returns;
    // }

    private double[] extractReturns(Dataset<Row> avgReturns) {
        // avgReturns의 컬럼 이름을 알파벳 순으로 정렬합니다.
        List<String> columnNames = Arrays.asList(avgReturns.columns());
        Collections.sort(columnNames);
    
        // avgReturns에 한 행만 있다고 가정합니다.
        Row row = avgReturns.collectAsList().get(0);
    
        // 컬럼 개수에 맞는 배열 생성
        double[] returns = new double[columnNames.size()];
    
        // 알파벳 순으로 정렬된 컬럼 순서대로 값을 추출
        for (int i = 0; i < columnNames.size(); i++) {
            // 컬럼 이름에 맞는 값을 추출
            returns[i] = row.getDouble(Arrays.asList(avgReturns.columns()).indexOf(columnNames.get(i)));
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

    public Dataset<Row> calculateAnnualReturn(Dataset<Row> df) {
        // 컬럼명 리스트 가져오기
        // aggregatedColumns: Map<String, Column>를 생성하는 부분
        Map<String, Column> aggregatedColumns = Arrays.stream(df.columns())
            .collect(Collectors.toMap(
                col -> col.replace("daily_ret", "annual_ret"),  // 컬럼명 변경
                col -> functions.avg(col).multiply(365)           // 평균 계산 후 365 곱하기
            ));
    
        // Map의 키를 알파벳 순으로 정렬
        List<String> sortedKeys = new ArrayList<>(aggregatedColumns.keySet());
        Collections.sort(sortedKeys);
    
        // 정렬된 키 순서대로 Column 배열을 생성
        Column[] aggColumns = sortedKeys.stream()
            .map(key -> aggregatedColumns.get(key).alias(key))
            .toArray(Column[]::new);
    
        // agg() 메서드는 varargs 형식으로 받으므로, 첫 번째 원소와 나머지 원소들을 분리하여 전달합니다.
        Dataset<Row> annualReturns;
        if (aggColumns.length > 0) {
            annualReturns = df.agg(aggColumns[0], Arrays.copyOfRange(aggColumns, 1, aggColumns.length));
        } else {
            // 처리할 컬럼이 없는 경우에 대한 처리 (예: 빈 데이터프레임 반환)
            annualReturns = df;
        }
    
        return annualReturns;
    }
    
    public static List<Map<String, Object>> runBackTestTrade(List<Map<String, Object>> testHistory, double current) {
        List<Map<String, Object>> result = new ArrayList<>();
        Trade trade = new Trade(current);
            
        for (Map<String, Object> entry : testHistory) {
            
            if (entry.get("buySignal") != null) {
                if (trade.getCurrent() > 10) {
                    trade.setTradePrice((double) entry.get("tradePrice"));
                    trade.setTickerCount(trade.getCurrent() / trade.getTradePrice());
                    trade.setCurrent(0);
                }
            } else if (entry.get("sellSignal") != null) {
                if (trade.getTickerCount() > 0) {
                    trade.setCurrent(trade.getTickerCount() * (double) entry.get("tradePrice"));
                    trade.setTradePrice((double) entry.get("tradePrice"));
                    trade.setTickerCount(0);
                }
            }

            // if (RSI < 20) {
            //     if (trade.getCurrent() > 10) {
            //         trade.setTradePrice((double) entry.get("tradePrice"));
            //         trade.setTickerCount(trade.getCurrent() / trade.getTradePrice());
            //         trade.setCurrent(0);
            //     }
            // } else if (RSI > 80) {
            //     if (trade.getTickerCount() > 0) {
            //         trade.setCurrent(trade.getTickerCount() * (double) entry.get("tradePrice"));
            //         trade.setTradePrice((double) entry.get("tradePrice"));
            //         trade.setTickerCount(0);
            //     }
            // }
    
            // trade 객체를 Map으로 변환하여 저장
            Map<String, Object> tradeInfo = new HashMap<>();
            tradeInfo.put("current", trade.getCurrent());
            tradeInfo.put("tickerCount", trade.getTickerCount());
            tradeInfo.put("tradePrice", trade.getTradePrice());
            result.add(tradeInfo);
        }
        return result;
    }

    public static Map<String, Double> calculateFinalValue(List<Map<String, Object>> testResult, List<OHLCData> parsedData) {
        
        OHLCData finalData = parsedData.get(parsedData.size() -1);
        double finalTradePrice = finalData.getTradePrice();

        Map<String, Double> result = new HashMap<>();
        double finalValue = 0;
        
        Map<String, Object> lastElement = testResult.get(testResult.size() - 1);
        String ticker = finalData.getTicker();
        if ((double) lastElement.get("tickerCount") != 0 ) {
            finalValue = (double) lastElement.get("tickerCount") * finalTradePrice + (double) lastElement.get("current");
        } else {
            finalValue = (double) lastElement.get("current");
        }
        
        result.put(ticker, finalValue);

        return result;
    }


}