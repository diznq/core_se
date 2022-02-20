package exchange.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class OrderBook {
    final AtomicLong lastPrice = new AtomicLong(100L);
    @JsonIgnore
    final PriorityQueue<Order> bid = new PriorityQueue<>(Order::inverseCompare);
    @JsonIgnore
    final PriorityQueue<Order> ask = new PriorityQueue<>(Order::compare);
    @JsonIgnore
    final Object sync = new Object();
    @JsonIgnore
    final Logger log = LoggerFactory.getLogger(OrderBook.class);

    final List<Tx> prices = Collections.synchronizedList(new ArrayList<>());
    long left = 0L;
    long right = 0L;
    String name = "EURXCC";

    public OrderBook(long baseId, long quoteId, String name) {
        this.name = name;
        this.left = baseId;
        this.right = quoteId;
    }

    void placeBid(Order order) {
        synchronized (sync) {
            bid.add(order);
        }
    }

    void placeAsk(Order order) {
        synchronized (sync) {
            ask.add(order);
        }
    }

    long determinePrice(Order a, Order b) {
        return a.id < b.id ? a.price : b.price;
    }

    long matchMultiple(AssetRepository repo) {
        long matched = 0L;
        final List<Order> bidsToRemove = new LinkedList<>();
        final List<Order> asksToRemove = new LinkedList<>();
        final long repoTime = repo.getTime();
        for (Order bidPeek : bid) {
            if (bidPeek.isCancellable() && bidPeek.ttl != 0L && repoTime > bidPeek.ttl) {
                bidsToRemove.add(bidPeek);
                continue;
            }
            if (!repo.hasEnough(bidPeek.account, right, bidPeek.price * bidPeek.volume)) {
                //log.error("Bidder {} doesn't have promised volume of {} {} to make a purchase", repo.getSymbol
                // (bidPeek.account), bidPeek.price * bidPeek.volume, repo.getSymbol(right));
                bidsToRemove.add(bidPeek);
                continue;
            }
            for (Order askPeek : ask) {
                if (askPeek.isCancellable() && askPeek.ttl != 0L && repoTime > askPeek.ttl) {
                    asksToRemove.add(askPeek);
                    continue;
                }
                if (!repo.hasEnough(askPeek.account, left, askPeek.volume)) {
                    //log.error("Asker {} doesn't have promised volume of {} {} to sell", repo.getSymbol(askPeek
                    // .account), askPeek.volume, repo.getSymbol(left));
                    asksToRemove.add(askPeek);
                    continue;
                }
                if (bidPeek.price >= askPeek.price) {
                    long price = determinePrice(bidPeek, askPeek);
                    long transferVolume = Long.min(bidPeek.volume, askPeek.volume);
                    if (transferVolume == 0L) continue;
                    askPeek.touch();
                    bidPeek.touch();
                    askPeek.increaseVolume(-transferVolume);
                    bidPeek.increaseVolume(-transferVolume);
                    if (askPeek.getVolume() == 0L) asksToRemove.add(askPeek);
                    if (bidPeek.getVolume() == 0L) bidsToRemove.add(bidPeek);
                    repo.transferTo(bidPeek.account, right, -price * transferVolume, "bid-right-rem");
                    repo.transferTo(bidPeek.account, left, transferVolume, "bid-left-add");
                    repo.transferTo(askPeek.account, right, price * transferVolume, "ask-right-add");
                    repo.transferTo(askPeek.account, left, -transferVolume, "ask-left-rem");
                    lastPrice.set(price);
                    Tx tx = new Tx(repoTime, price, transferVolume);
                    prices.add(tx);
                    askPeek.onFullfilled(tx);
                    bidPeek.onFullfilled(tx);
                    matched++;
                } else {
                    break;
                }
            }
        }
        bid.removeAll(bidsToRemove);
        ask.removeAll(asksToRemove);
        return matched;
    }

    long tick(AssetRepository repository) {
        long matches = 0L;
        synchronized (sync) {
            matches += matchMultiple(repository);
        }
        return matches;
    }

    private Map<Long, Long> summarizeSide(final PriorityQueue<Order> orders) {
        Map<Long, Long> summary = new HashMap<>();
        for (Order order : orders) {
            if (!summary.containsKey(order.price)) {
                summary.put(order.price, 0L);
            }
            summary.put(order.price, summary.get(order.price) + order.volume);
        }
        return summary;
    }

    public Map<Long, Long> getBids() {
        return summarizeSide(bid);
    }

    public Map<Long, Long> getAsks() {
        return summarizeSide(ask);
    }

    public long getLeft() {
        return left;
    }

    public long getRight() {
        return right;
    }

    public long getLastPrice() {
        return lastPrice.get();
    }

    public List<Tx> getPriceHistory() {
        return prices.stream().toList();
    }

    public String getName() {
        return name;
    }

    public Order getTopBid() {
        for (Order order : bid) {
            return order;
        }
        return null;
    }

    public Order getTopAsk() {
        for (Order order : ask) {
            return order;
        }
        return null;
    }
}
