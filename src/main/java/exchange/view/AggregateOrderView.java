package exchange.view;

public class AggregateOrderView {
    public long price;
    public long volume;

    public AggregateOrderView(long price, long volume) {
        this.price = price;
        this.volume = volume;
    }

    public int compare(AggregateOrderView other) {
        return Long.compare(price, other.price);
    }

    public int inverseCompare(AggregateOrderView other) {
        return Long.compare(other.price, price);
    }
}
