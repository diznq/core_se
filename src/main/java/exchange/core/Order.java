package exchange.core;

import java.util.function.Consumer;

public class Order {
    long id;
    long price;
    long volume;
    long account;
    long ttl = 0L;
    boolean touched = false;
    Consumer<Tx> callback;

    public Order(long price, long volume, long account) {
        this.price = price;
        this.volume = volume;
        this.account = account;
    }

    public Order(long price, long volume, long account, Consumer<Tx> callback) {
        this.price = price;
        this.volume = volume;
        this.account = account;
        this.callback = callback;
    }

    public Order(long price, long volume, long account, long id) {
        this.price = price;
        this.volume = volume;
        this.account = account;
        this.id = id;
    }

    public int compare(Order other) {
        int priceResult = Long.compare(price, other.price);
        if (priceResult == 0) {
            return Long.compare(id, other.id);
        }
        return priceResult;
    }

    public int inverseCompare(Order other) {
        int priceResult = Long.compare(other.price, price);
        if (priceResult == 0) {
            return Long.compare(id, other.id);
        }
        return priceResult;
    }

    public long getPrice() {
        return price;
    }

    public long getVolume() {
        return volume;
    }

    public long getAccount() {
        return account;
    }

    public long increaseVolume(long volume) {
        this.volume += volume;
        return this.volume;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isCancellable() {
        return true;
    }

    public void touch() {
        touched = true;
    }

    public boolean isTouched() {
        return touched;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public long getTtl() {
        return ttl;
    }

    public void onFullfilled(Tx tx) {
        if (callback != null) {
            callback.accept(tx);
        }
    }

    public void setCallback(Consumer<Tx> callback) {
        this.callback = callback;
    }

    public Consumer<Tx> getCallback() {
        return callback;
    }
}