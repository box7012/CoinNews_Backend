package app.message.demo1;

public class OHLCData {
    private long timestamp;
    private double openingPrice;
    private double highPrice;
    private double lowPrice;
    private double tradePrice;
    private String ticker;
    private Double buySignal;  // 매수 신호 (nullable)
    private Double sellSignal; // 매도 신호 (nullable)

    public OHLCData(long timestamp, double openingPrice, double highPrice, double lowPrice, double tradePrice, String ticker) {
        this.timestamp = timestamp;
        this.openingPrice = openingPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.tradePrice = tradePrice;
        this.ticker = ticker;
        this.buySignal = null;  // 기본값 null
        this.sellSignal = null; // 기본값 null
    }

    // Getter 및 Setter 추가
    public Double getBuySignal() { return buySignal; }
    public void setBuySignal(Double buySignal) { this.buySignal = buySignal; }

    public Double getSellSignal() { return sellSignal; }
    public void setSellSignal(Double sellSignal) { this.sellSignal = sellSignal; }

    // 기존 getter들
    public long getTimestamp() { return timestamp; }
    public double getOpeningPrice() { return openingPrice; }
    public double getHighPrice() { return highPrice; }
    public double getLowPrice() { return lowPrice; }
    public double getTradePrice() { return tradePrice; }
    public String getTicker() { return ticker; }

    public long getTime() { return timestamp; }
}
