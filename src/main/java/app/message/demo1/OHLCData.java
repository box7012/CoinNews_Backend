package app.message.demo1;

public class OHLCData {
    private long timestamp;
    private double openingPrice;
    private double highPrice;
    private double lowPrice;
    private double tradePrice;
    private String ticker;
    private Boolean buySignal;  // 매수 신호 (Boolean)
    private Boolean sellSignal; // 매도 신호 (Boolean)

    public OHLCData(long timestamp, double openingPrice, double highPrice, double lowPrice, double tradePrice, String ticker) {
        this.timestamp = timestamp;
        this.openingPrice = openingPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.tradePrice = tradePrice;
        this.ticker = ticker;
        this.buySignal = true;  // 기본값 true
        this.sellSignal = true; // 기본값 true
    }

    // Getter 및 Setter 추가
    public Boolean getBuySignal() { return buySignal; }
    public void setBuySignal(Boolean buySignal) { this.buySignal = buySignal; }

    public Boolean getSellSignal() { return sellSignal; }
    public void setSellSignal(Boolean sellSignal) { this.sellSignal = sellSignal; }

    // 기존 getter들
    public long getTimestamp() { return timestamp; }
    public double getOpeningPrice() { return openingPrice; }
    public double getHighPrice() { return highPrice; }
    public double getLowPrice() { return lowPrice; }
    public double getTradePrice() { return tradePrice; }
    public String getTicker() { return ticker; }

    public long getTime() { return timestamp; }
    
    @Override
    public String toString() {
        return "OHLCData{" +
            "timestamp=" + timestamp +
            ", openingPrice=" + openingPrice +
            ", highPrice=" + highPrice +
            ", lowPrice=" + lowPrice +
            ", tradePrice=" + tradePrice +
            ", ticker='" + ticker + '\'' +
            ", buySignal=" + buySignal +
            ", sellSignal=" + sellSignal +
            '}';
    }
}
