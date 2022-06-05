package exchange.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
        return a.compareAtSamePrice(b) < 0 ? a.price : b.price;
    }

    Mono<Long> matchMultiple(AssetRepository repo, final long repoTime) {
        return Flux.fromIterable(bid)
                .zipWith(Flux.fromIterable(ask))
                .filter(tuple -> {
                    Order bidPeek = tuple.getT1();
                    Order askPeek = tuple.getT2();
                    long transferVolume = Long.min(bidPeek.getRealVolume(), askPeek.getRealVolume());
                    return transferVolume > 0L && bidPeek.getPrice() >= askPeek.getPrice();
                })
                .flatMap(tuple -> {
                    Order bidPeek = tuple.getT1();
                    Order askPeek = tuple.getT2();
                    long price = determinePrice(bidPeek, askPeek);
                    long transferVolume = Long.min(bidPeek.getRealVolume(), askPeek.getRealVolume());
                    askPeek.fill(transferVolume);
                    bidPeek.fill(transferVolume);
                    return repo
                            .transferTo(bidPeek.account, right, -price * transferVolume, "bid-right-rem")
                            .zipWith(repo.transferTo(bidPeek.account, left, transferVolume, "bid-left-add"))
                            .zipWith(repo.transferTo(askPeek.account, right, price * transferVolume, "ask-right-add"))
                            .zipWith(repo.transferTo(askPeek.account, left, -transferVolume, "ask-left-rem"))
                            .flatMap(result -> {
                                Tx tx = new Tx(repoTime, price, transferVolume);
                                prices.add(tx);
                                askPeek.onFulfilled(tx);
                                bidPeek.onFulfilled(tx);
                                sink.tryEmitNext(tx);
                                lastPrice.set(price);
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
}
