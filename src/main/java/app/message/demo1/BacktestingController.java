package app.message.demo1;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYPolygonAnnotation;
import org.jfree.chart.axis.DateAxis;

import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.DefaultOHLCDataset;
import org.jfree.data.xy.OHLCDataItem;
import org.jfree.data.xy.OHLCDataset;



@Controller
@RequestMapping("/api")
// @CrossOrigin(origins = "http://192.168.0.2:8080")
@CrossOrigin(origins = {"https://coin-dashboard.xyz", "http://192.168.0.2:8080", "http://localhost:5173"})
public class BacktestingController {

    private final Log log = LogFactory.getLog(BacktestingController.class);

    @Autowired
    private BacktestingService backtestingService;

    @Autowired
    private BackTester backTester;

    @PostMapping("/analysis")
    @ResponseBody
    public Map<String, Object> analyze(@RequestBody Map<String, Object> requestData) {
        Map<String, Object> result = new HashMap<>();
    
        // 요청된 티커 목록 가져오기
        List<String> tickers = (List<String>) requestData.get("tickers");
        String startDate = (String) requestData.get("startDate");
        String endDate = (String) requestData.get("endDate");
        List<Map<String, String>> strategyList = (List<Map<String, String>>) requestData.get("strategies");
        List<String> selectedStrategyList = strategyList.stream()
            .map(strategy -> strategy.get("selected"))
            .collect(Collectors.toList());

        Map<String, Map<String, String>> conditions = (Map<String, Map<String, String>>) requestData.get("parameters");

        // 알파벳순 정렬
        Collections.sort(tickers);
    
        // 각 티커에 대한 그래프를 저장할 리스트
        List<OHLCData> allOhlcData = new ArrayList<>();
        List<String> graphs = new ArrayList<>();
        List<List<Map<String,Object>>> backTestingHistory = new ArrayList<>();
        List<List<Map<String,Object>>> backTestingResult = new ArrayList<>();
        List<Map<String, Double>> finalValueList = new ArrayList<>();
        
        Map<String, BiFunction<List<OHLCData>, Map<String, String>, Dataset<Row>>> strategyMap = new HashMap<>();
        strategyMap.put("RSI", backTester::backTestingRSIDataset);
        strategyMap.put("Psychological Line", backTester::backTestingPSYDataset);
        strategyMap.put("CCI", backTester::backTestingCCIDataset);
        strategyMap.put("MACD", backTester::backTestingMACDDataset);
        strategyMap.put("Bollinger Band", backTester::backTestingBollingerDataset);
                       
        // 각 티커에 대해 Binance API 데이터를 모으기
        for (String ticker : tickers) {
            String symbol = convertToBinanceSymbol(ticker);
            String url = "https://api.binance.com/api/v3/klines?symbol=" + symbol + "&interval=1d&limit=100";
            
            // Binance API에서 OHLC 데이터 가져오기
            String ohlcData = fetchOhlcData(url);
            if (ohlcData != null) {
                // 데이터를 파싱해서 리스트에 저장
                List<OHLCData> parsedData = parseBinanceData(ohlcData, ticker);

                long sD, eD;
                if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
                    sD = dateToMilliSec(startDate);
                    eD = dateToMilliSec(endDate);
                } else { // 하나라도 없으면 그냥 오늘, 100일전으로 바꿈
                    LocalDate today = LocalDate.now();
                    LocalDate hundredDaysAgo = today.minusDays(100);
                    long todayTimestamp = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    long hundredDaysAgoTimestamp = hundredDaysAgo.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    sD = hundredDaysAgoTimestamp;
                    eD = todayTimestamp;
                }

                // 날짜로 자른 데이터
                List<OHLCData> betweenDateParsedData = parsedData.stream()
                    .filter(data -> data.getTime() >= sD && data.getTime() <= eD)
                    .collect(Collectors.toList());

                List<Dataset<Row>> signalWithStrategy = new ArrayList<>();
                Map<String, String> condition = new HashMap<>();
                
                
                for (String strategy : selectedStrategyList) {
                    BiFunction<List<OHLCData>, Map<String, String>, Dataset<Row>> function = strategyMap.get(strategy);
                    if (function != null) {
                        condition = conditions.get(strategy);
                        // check!
                        Dataset<Row> resultDf = function.apply(betweenDateParsedData, condition);
                        signalWithStrategy.add(resultDf);
                    }
                }

                

                Dataset<Row> finalResultDf;
                List<Map<String, Object>> signalAddedList = new ArrayList<>(); 
                // 병합할 데이터가 있는지 확인
                if (!signalWithStrategy.isEmpty()) {
                    // 첫 번째 DF를 기준으로 함
                    finalResultDf = signalWithStrategy.get(0);

                    // 나머지 DF를 순차적으로 병합 (full_outer join 사용)
                    for (int i = 1; i < signalWithStrategy.size(); i++) {
                        finalResultDf = finalResultDf.join(signalWithStrategy.get(i), 
                                                        "timestamp", // 공통 기준 컬럼
                                                        "full_outer"); // 컬럼이 다를 경우 자동 추가
                    }

                    // Dataset<Row> → List<Map<String, Object>> 변환
                    signalAddedList = finalResultDf.collectAsList().stream()
                        .map(row -> {
                            Map<String, Object> map = new HashMap<>();
                            for (String field : row.schema().fieldNames()) {
                                map.put(field, row.getAs(field));
                            }
                            return map;
                        })
                        .collect(Collectors.toList());
                }
                
                //check - 만약 거래가 일어나지 않으면 에러생김 예외처리가 필요할거같고
                List<Map<String, Object>> testHistory = signalAddedList.stream()
                    .filter(map -> Boolean.TRUE.equals(map.get("buySignal")) || Boolean.TRUE.equals(map.get("sellSignal")))
                    .collect(Collectors.toList());

                // for (Map<String, Object> data : testHistory) {
                //     System.out.println(data);
                // }
                // log.info("goodgood");


                backTestingHistory.add(testHistory); 
                // 지금 현금 100000 으로 시작하지만, 이거는 리퀘스트에서 값 추출해서 넣어야함
                List<Map<String, Object>> testResult = backtestingService.runBackTestTrade(testHistory, 100000);
                backTestingResult.add(testResult);
                finalValueList.add(backtestingService.calculateFinalValue(testResult, betweenDateParsedData));

                graphs.add(generateCandleChartBase64(symbol, betweenDateParsedData, testHistory));
                // 모든 데이터를 모음
                allOhlcData.addAll(betweenDateParsedData);  

            }
        }
    
        // 한 번에 runMonteCarloSimulation() 호출 (모은 데이터로 시뮬레이션 수행)
        if (!allOhlcData.isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonAllOhlcData = objectMapper.writeValueAsString(allOhlcData);
                String simulationResult = backtestingService.runMonteCarloSimulation(jsonAllOhlcData);
                // runMonteCarloSimulation을 한 번만 호출
                graphs.add(0, simulationResult);
            } catch (JsonProcessingException e) {
                e.printStackTrace();  // 예외 처리
            }
        }
    
        result.put("message", "✅ 분석 완료");
        // 그래프 결과 리스트에 추가
        result.put("graphs", graphs);
        result.put("backTestHistory", backTestingHistory);
        result.put("backTestResults", backTestingResult);
        result.put("finalValueList", finalValueList);
        
        return result;
    }

    // Binance 티커 변환 (ex: BTC → BTCUSDT)
    private String convertToBinanceSymbol(String ticker) {
        return ticker + "USDT"; // Binance는 "BTCUSDT" 형식 사용
    }

    private List<OHLCData> parseBinanceData(String binanceResponse, String ticker) {
        List<OHLCData> ohlcDataList = new ArrayList<>();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<List<Object>> rawData = objectMapper.readValue(binanceResponse, List.class);

            for (List<Object> entry : rawData) {
                long timestamp = ((Number) entry.get(0)).longValue(); // K선 시작 시간
                double openingPrice = Double.parseDouble(entry.get(1).toString());
                double highPrice = Double.parseDouble(entry.get(2).toString());
                double lowPrice = Double.parseDouble(entry.get(3).toString());
                double tradePrice = Double.parseDouble(entry.get(4).toString()); // 종가

                ohlcDataList.add(new OHLCData(timestamp, openingPrice, highPrice, lowPrice, tradePrice, ticker));
            }
        } catch (Exception e) {
            log.error("❌ Binance 데이터 파싱 오류", e);
        }

        return ohlcDataList;
    }

    // Binance API에서 OHLC 데이터 요청
    private String fetchOhlcData(String url) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url, String.class);
            return response;
        } catch (Exception e) {
            log.error("❌ Binance API 요청 실패", e);
            return null;
        }
    }

    private String generateCandleChartBase64(String symbol, List<OHLCData> ohlcDataList, List<Map<String, Object>> testHistory) {
        if (ohlcDataList == null || ohlcDataList.isEmpty()) {
            return null;
        }
    
        // OHLCData를 JFreeChart의 OHLCDataItem으로 변환
        OHLCDataItem[] dataItems = new OHLCDataItem[ohlcDataList.size()];
        for (int i = 0; i < ohlcDataList.size(); i++) {
            OHLCData ohlcData = ohlcDataList.get(i);
            dataItems[i] = new OHLCDataItem(
                    new java.util.Date(ohlcData.getTimestamp()),
                    ohlcData.getOpeningPrice(),
                    ohlcData.getHighPrice(),
                    ohlcData.getLowPrice(),
                    ohlcData.getTradePrice(),
                    0 // Volume은 0으로 설정 (필요시 수정)
            );
        }
    
        // 데이터셋 생성
        OHLCDataset dataset = new DefaultOHLCDataset("Candlestick", dataItems);
    
        // 캔들 차트 생성
        JFreeChart chart = ChartFactory.createCandlestickChart(
                extractTicker(symbol) + " candlestick chart", // 차트 제목
                "Time",              // X축 레이블
                "Price ($)",             // Y축 레이블
                dataset,             // 데이터셋
                false                // 범례 표시 여부
        );
    
        // 차트 스타일 설정
        XYPlot plot = (XYPlot) chart.getPlot();
        CandlestickRenderer renderer = new CandlestickRenderer();
        renderer.setAutoWidthMethod(CandlestickRenderer.WIDTHMETHOD_SMALLEST);
        plot.setRenderer(renderer);
    
        // X축 날짜 형식 설정
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("MM-dd HH:mm"));
    
        // Y축 범위 설정 (데이터의 최소값과 최대값을 기준으로 조정)
        double minPrice = Double.MAX_VALUE;
        double maxPrice = Double.MIN_VALUE;
        for (OHLCData ohlcData : ohlcDataList) {
            if (ohlcData.getLowPrice() < minPrice) {
                minPrice = ohlcData.getLowPrice();
            }
            if (ohlcData.getHighPrice() > maxPrice) {
                maxPrice = ohlcData.getHighPrice();
            }
        }
    
        // Y축 범위를 데이터의 최소값과 최대값에 맞게 설정
        double margin = (maxPrice - minPrice) * 0.1; // 10% 여유 공간 추가
        plot.getRangeAxis().setRange(new Range(minPrice - margin, maxPrice + margin));

        addTradeSignals(plot, minPrice, maxPrice, testHistory);

        // 차트를 BufferedImage로 변환
        BufferedImage image = chart.createBufferedImage(800, 600);
    
        // BufferedImage를 Base64로 인코딩
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ChartUtils.writeBufferedImageAsPNG(outputStream, image);
            byte[] imageBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            log.error("❌ 차트 이미지 생성 실패", e);
            return null;
        }
    }

    public static String extractTicker(String ticker) {
        if (ticker != null && ticker.endsWith("USDT")) {
            return ticker.substring(0, ticker.length() - 4);  // "USDT"를 제거하고 반환
        }
        return ticker;  // "USDT"가 없으면 그대로 반환
    }

    private long dateToMilliSec(String dateString) {
        LocalDate localDate = LocalDate.parse(dateString);
        return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private void addTradeSignals(XYPlot plot, double minPrice, double maxPrice, List<Map<String, Object>> testHistory) {
        double sizeX = 1000 * 60 * 30; // X축 크기 (예: 30분 단위)
        double sizeY = (maxPrice - minPrice) * 0.04; // Y축 크기 (5% 비율, 삼각형 크기 확대) 100개니까?
        
        for (Map<String, Object> entry : testHistory) {
            long x = ((Number) entry.get("timestamp")).longValue(); // timestamp를 long으로 변환
            double y = ((Number) entry.get("tradePrice")).doubleValue(); // tradePrice를 double로 변환
            double highPrice = ((Number) entry.get("highPrice")).doubleValue(); // highPrice 값 가져오기
            double lowPrice = ((Number) entry.get("lowPrice")).doubleValue(); // lowPrice 값 가져오기
            
            // 매수 신호 🔵 (lowPrice에 표시)
            if (Boolean.TRUE.equals(entry.get("buySignal"))) {
                double[] triangle = {
                    x, lowPrice ,  // 아래쪽 꼭대기 (lowPrice에서 5% 아래)
                    x - 30 * sizeX, lowPrice - sizeY / 2,  // 왼쪽 아래
                    x + 30 * sizeX, lowPrice - sizeY / 2   // 오른쪽 아래
                };
                plot.addAnnotation(new XYPolygonAnnotation(triangle, new BasicStroke(2f), Color.BLUE, Color.BLUE)); // 삼각형 크기 키우기
            }
        
            // 매도 신호 🟡 (highPrice에 표시)
            if (Boolean.TRUE.equals(entry.get("sellSignal"))) {
                double[] triangle = {
                    x, highPrice,  // 위쪽 꼭대기 (highPrice에서 5% 위)
                    x - 30 * sizeX, highPrice + sizeY / 2,  // 왼쪽 아래
                    x + 30 * sizeX, highPrice + sizeY / 2   // 오른쪽 아래
                };
                plot.addAnnotation(new XYPolygonAnnotation(triangle, new BasicStroke(2f), Color.YELLOW, Color.YELLOW)); // 삼각형 크기 키우기
            }
        }
    }
    
}


