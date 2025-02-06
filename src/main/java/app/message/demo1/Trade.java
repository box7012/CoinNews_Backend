package app.message.demo1;

public class Trade {

    private double current;
    private double tickerCount;
    private double tradePrice;
    
    public Trade(double current) {
        this.current = current;
        this.tickerCount = 0;
    }

    public void setCurrent(double current) {
        this.current = current;
    }

    public double getCurrent() {
        return this.current;
    }

    public void setTickerCount(double tickerCount) {
        this.tickerCount = tickerCount;
    }

    public double getTickerCount() {
        return this.tickerCount;
    }

    public void setTradePrice(double tradePrice) {
        this.tradePrice = tradePrice;
    }

    public double getTradePrice() {
        return this.tradePrice;
    }


}
