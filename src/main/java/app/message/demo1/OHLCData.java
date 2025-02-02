package app.message.demo1;

public class OHLCData {
    private long timestamp;
    private double openingPrice;
    private double highPrice;
    private double lowPrice;
    private double tradePrice;
    private String ticker;  // ticker 필드 추가

    public OHLCData(long timestamp, double openingPrice, double highPrice, double lowPrice, double tradePrice, String ticker) {
        this.timestamp = timestamp;
        this.openingPrice = openingPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.tradePrice = tradePrice;
        this.ticker = ticker;  // ticker 값 저장
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    // 기존 getter들
    public long getTimestamp() { return timestamp; }
    public double getOpeningPrice() { return openingPrice; }
    public double getHighPrice() { return highPrice; }
    public double getLowPrice() { return lowPrice; }
    public double getTradePrice() { return tradePrice; }
    public String getTicker() { return ticker; }

    // 추가한 getTime() 메서드
    public long getTime() {
        return timestamp; // timestamp를 그대로 반환
    }
}