package app.message.demo1;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

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
@CrossOrigin(origins = {"https://coin-dashboard.xyz", "http://192.168.0.2:8080"})
public class BacktestingController {

    private final Log log = LogFactory.getLog(BacktestingController.class);

    @Autowired
    private BacktestingService backtestingService;

    @PostMapping("/analysis")
    @ResponseBody
    public Map<String, Object> analyze(@RequestBody Map<String, Object> requestData) {
        Map<String, Object> result = new HashMap<>();
    
        // 요청된 티커 목록 가져오기
        List<String> tickers = (List<String>) requestData.get("tickers");
    
        // 각 티커에 대한 그래프를 저장할 리스트
        List<OHLCData> allOhlcData = new ArrayList<>();
        List<String> graphs = new ArrayList<>();
        
        // 각 티커에 대해 Binance API 데이터를 모으기
        for (String ticker : tickers) {
            String symbol = convertToBinanceSymbol(ticker);
            String url = "https://api.binance.com/api/v3/klines?symbol=" + symbol + "&interval=1d&limit=100";
            
            // Binance API에서 OHLC 데이터 가져오기
            String ohlcData = fetchOhlcData(url);
            if (ohlcData != null) {
                // 데이터를 파싱해서 리스트에 저장
                List<OHLCData> parsedData = parseBinanceData(ohlcData, ticker);
                graphs.add(generateCandleChartBase64(symbol, parsedData));
                allOhlcData.addAll(parsedData);  // 모든 데이터를 모음
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

    private String generateCandleChartBase64(String symbol, List<OHLCData> ohlcDataList) {
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
    
        // 🔵 파란색 삼각형(매수) 추가 (마지막 데이터 기준)
        if (!ohlcDataList.isEmpty()) {
            int lastIndex = ohlcDataList.size() - 1;
            OHLCData lastData = ohlcDataList.get(lastIndex);
            
            double x = lastData.getTimestamp(); // X축 (시간)
            double y = lastData.getTradePrice(); // Y축 (가격)

            double sizeX = 1000 * 60 * 60; // X축 크기 (예: 1시간 단위)
            double sizeY = (maxPrice - minPrice) * 0.02; // Y축 크기 (2% 비율)

            double[] triangle = {
                x, y + sizeY,  // 꼭대기
                x - 20 * sizeX, y - (sizeY / 2),  // 왼쪽 아래
                x + 20 * sizeX, y -  (sizeY / 2)  // 오른쪽 아래
            };

            XYPolygonAnnotation annotation = new XYPolygonAnnotation(
                triangle, new BasicStroke(1.5f), Color.BLUE, Color.BLUE
            );
            plot.addAnnotation(annotation);
        }

        // 🟡 노란색 삼각형(매도) 추가 (마지막 데이터 기준)
        if (!ohlcDataList.isEmpty()) {
            int lastIndex = ohlcDataList.size() - 1;
            OHLCData lastData = ohlcDataList.get(lastIndex);
            
            double x = lastData.getTimestamp(); // X축 (시간)
            double y = lastData.getOpeningPrice(); // Y축 (가격)

            double sizeX = 1000 * 60 * 60; // X축 크기 (예: 1시간 단위)
            double sizeY = (maxPrice - minPrice) * 0.02; // Y축 크기 (2% 비율)

            double[] triangle = {
                x, y - sizeY,  // 꼭대기
                x - 20 * sizeX, y + (sizeY / 2),  // 왼쪽 아래
                x + 20 * sizeX, y +  (sizeY / 2)  // 오른쪽 아래
            };

            XYPolygonAnnotation annotation = new XYPolygonAnnotation(
                triangle, new BasicStroke(1.5f), Color.YELLOW, Color.YELLOW
            );
            plot.addAnnotation(annotation);
        }

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




}
