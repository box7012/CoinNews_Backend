package app.message.demo1;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
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
@CrossOrigin(origins = "https://coin-dashboard.xyz/")
public class BacktestingController {

    private final Log log = LogFactory.getLog(BacktestingController.class);

    @PostMapping("/analysis")
    @ResponseBody
    public Map<String, Object> analyze(@RequestBody Map<String, Object> requestData) {
        Map<String, Object> result = new HashMap<>();

        // 요청된 티커 목록 가져오기
        List<String> tickers = (List<String>) requestData.get("tickers");

        // 각 티커에 대한 그래프 생성
        List<String> graphs = new ArrayList<>();
        for (String ticker : tickers) {
            String symbol = convertToBinanceSymbol(ticker);
            String url = "https://api.binance.com/api/v3/klines?symbol=" + symbol + "&interval=1d&limit=100";
            // Binance API에서 OHLC 데이터 가져오기
            String ohlcData = fetchOhlcData(url);
            if (ohlcData != null) {
                List<OHLCData> parsedData = parseBinanceData(ohlcData);
                String graphBase64 = generateCandleChartBase64(symbol, parsedData);
                if (graphBase64 != null) {
                    graphs.add(graphBase64);
                }
            }
        }

        result.put("message", "✅ 분석 완료");
        result.put("graphs", graphs);  // 각 티커에 대한 그래프 Base64 반환

        return result;
    }

    // Binance 티커 변환 (ex: BTC → BTCUSDT)
    private String convertToBinanceSymbol(String ticker) {
        return ticker + "USDT"; // Binance는 "BTCUSDT" 형식 사용
    }

    private List<OHLCData> parseBinanceData(String binanceResponse) {
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

                ohlcDataList.add(new OHLCData(timestamp, openingPrice, highPrice, lowPrice, tradePrice));
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
