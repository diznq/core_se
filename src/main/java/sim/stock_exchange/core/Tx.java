package sim.stock_exchange.core;

public class Tx {
    public long time;
    public long price;
    public long volume;

    public Tx(long time, long price, long volume) {
        this.time = time;
        this.price = price;
        this.volume = volume;
    }

    public int compare(Tx other) {
        return Long.compare(price, other.price);
    }
}
