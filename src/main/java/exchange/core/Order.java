package exchange.core;

import lombok.Data;
import lombok.Getter;

import java.util.function.Consumer;

@Data
public class Order {
    public long id;
    @Getter
    long price;
    @Getter
    long priority;
    @Getter
    long filled;
    @Getter
    long volume;
    @Getter
    Account account;

    private Consumer<Tx> callback;

    public Order(long price, long volume, Account account) {
        this.price = price;
        this.filled = 0;
        this.volume = volume;
        this.account = account;
    }

    public long getRealVolume() {
        return volume - filled;
    }

    public int compare(Order other) {
        int priceResult = Long.compare(price, other.price);
        if (priceResult == 0) {
            return compareAtSamePrice(other);
        }
        return priceResult;
    }

    public int compareAtSamePrice(Order other) {
        int priorityResult = Long.compare(priority, other.priority);
        if (priorityResult == 0)
            return Long.compare(id, other.id);
        return -priorityResult;
    }

    public int inverseCompare(Order other) {
        return -compare(other);
    }

    public void fill(long volume) {
        this.filled += volume;
    }

    public void onFulfilled(Tx tx) {
        if (callback != null) {
            callback.accept(tx);
        }
    }
}