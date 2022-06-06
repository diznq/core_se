package exchange.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import exchange.model.Order;
import exchange.model.Tx;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class OrderBook {
    final AtomicLong lastPrice = new AtomicLong(0L);
    final PriorityQueue<Order> bid = new PriorityQueue<>(Order::inverseCompare);
    final PriorityQueue<Order> ask = new PriorityQueue<>(Order::compare);
    final Object sync = new Object();

    final List<Tx> prices = Collections.synchronizedList(new ArrayList<>());
    final Sinks.Many<Tx> sink = Sinks.many().multicast().onBackpressureBuffer();

    String left;
    String right;
    String name;

    public OrderBook(String baseId, String quoteId, String name) {
        this.name = name;
        this.left = baseId;
        this.right = quoteId;
    }

    Mono<Order> placeBid(Order order) {
        synchronized (sync) {
            bid.add(order);
            return Mono.just(order);
        }
    }

    Mono<Order> placeAsk(Order order) {
        synchronized (sync) {
            ask.add(order);
            return Mono.just(order);
        }
    }

    long determinePrice(Order a, Order b) {
        return a.compareAtSamePrice(b) < 0 ? a.getPrice() : b.getPrice();
    }

    Mono<Long> matchMultiple(AssetManager repo, final long repoTime) {
        final List<Pair> combinations = new LinkedList<>();
        final List<Order> bidsToRemove = new LinkedList<>();
        final List<Order> asksToRemove = new LinkedList<>();
        boolean endSearch = false;
        for (Order bidPeek : bid) {
            for (Order askPeek : ask) {
                if (bidPeek.getPrice() < askPeek.getPrice()) {
                    endSearch = true;
                    break;
                }
                long bidVolume = bidPeek.getRealVolume();
                long askVolume = askPeek.getRealVolume();
                if (bidVolume == 0L || askVolume == 0L) {
                    if (bidVolume == 0L) bidsToRemove.add(bidPeek);
                    if (askVolume == 0L) asksToRemove.add(askPeek);
                    continue;
                }
                combinations.add(new Pair(bidPeek, askPeek));
            }
            if (endSearch) break;
        }
        bid.removeAll(bidsToRemove);
        ask.removeAll(asksToRemove);

        return Flux.fromIterable(combinations)
                .flatMap(tuple -> {
                    Order bidPeek = tuple.bid;
                    Order askPeek = tuple.ask;
                    long price = determinePrice(bidPeek, askPeek);
                    long transferVolume = Long.min(bidPeek.getRealVolume(), askPeek.getRealVolume());
                    return repo
                            .transaction(bidPeek, askPeek, left, right, price, transferVolume)
                            .flatMap(result -> {
                                Tx tx = new Tx(repoTime, price, transferVolume);
                                prices.add(tx);
                                sink.tryEmitNext(tx);
                                return Mono.just(1L);
                            });
                })
                .reduce(Long::sum);
    }

    private Map<Long, Long> summarizeSide(final PriorityQueue<Order> orders) {
        Map<Long, Long> summary = new HashMap<>();
        for (Order order : orders) {
            if (!summary.containsKey(order.getPrice())) {
                summary.put(order.getPrice(), 0L);
            }
            summary.put(order.getPrice(), summary.get(order.getPrice()) + order.getRealVolume());
        }
        return summary;
    }

    public Map<Long, Long> getBids() {
        return summarizeSide(bid);
    }

    public Map<Long, Long> getAsks() {
        return summarizeSide(ask);
    }

    public String getLeft() {
        return left;
    }

    public String getRight() {
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

    @JsonIgnore
    public Sinks.Many<Tx> getSink() {
        return sink;
    }

    public Order getTopBid() {
        return bid.peek();
    }

    public Order getTopAsk() {
        return ask.peek();
    }

    public final class Pair {
        Order bid;
        Order ask;

        public Pair(Order bid, Order ask) {
            this.bid = bid;
            this.ask = ask;
        }
    }
}
