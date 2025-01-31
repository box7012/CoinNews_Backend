package app.message.demo1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OHLCData {
    private long timestamp;
    private double openingPrice;
    private double highPrice;
    private double lowPrice;
    private double tradePrice;

    public OHLCData(long timestamp, double openingPrice, double highPrice, double lowPrice, double tradePrice) {
        this.timestamp = timestamp;
        this.openingPrice = openingPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.tradePrice = tradePrice;
    }

    // 기존 getter들
    public long getTimestamp() { return timestamp; }
    public double getOpeningPrice() { return openingPrice; }
    public double getHighPrice() { return highPrice; }
    public double getLowPrice() { return lowPrice; }
    public double getTradePrice() { return tradePrice; }

    // 추가한 getTime() 메서드
    public long getTime() {
        return timestamp; // timestamp를 그대로 반환
    }
}