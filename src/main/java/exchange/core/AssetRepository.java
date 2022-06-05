package exchange.core;

import exchange.exc.InsufficientAssets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class AssetRepository {
    final Object accountingLock = new Object();
    Map<String, Account> accounts = new ConcurrentHashMap<>();
    Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
    AtomicLong idCounter = new AtomicLong(0L);
    AtomicLong orderIdCounter = new AtomicLong(0L);
    AtomicLong time = new AtomicLong(0L);

    public AssetRepository() {

    }

    public Mono<Boolean> transferTo(Account account, String asset, long volume, String reason) {
        synchronized (accountingLock) {
            if (account == null) return Mono.just(false);
            return account.transfer(asset, volume);
        }
    }

    public Mono<Account> getAccount(String name) {
        if (!accounts.containsKey(name)) {
            accounts.put(name, new Account(name));
        }
        return Mono.just(accounts.get(name));
    }

    public Mono<Long> getOrderId() {
        return Mono.just(orderIdCounter.getAndIncrement());
    }

    public Mono<OrderBook> getOrderBook(String base, String quote) {
        String common = base + quote;
        if (orderBooks.containsKey(common)) {
            return Mono.just(orderBooks.get(common));
        }
        OrderBook book = new OrderBook(base, quote, common);
        orderBooks.put(common, book);
        return Mono.just(book);
    }

    public Mono<Order> placeOrder(OrderBook orderBook, Order order, OrderSide side) {
        String lockAsset = side == OrderSide.ASK ? orderBook.getLeft() : orderBook.getRight();
        long lockVolume = side == OrderSide.ASK ? order.getVolume() : order.getVolume() * order.getPrice();
        return hasEnough(
                order.getAccount(),
                lockAsset,
                lockVolume
        )
                .zipWith(getOrderId())
                .flatMap(result -> {
                    boolean hasEnough = result.getT1();
                    long nextId = result.getT2();
                    if (!hasEnough) {
                        return Mono.error(new InsufficientAssets(lockAsset, lockVolume));
                    }
                    return reserve(order.getAccount(), lockAsset, lockVolume)
                            .flatMap(reserveOk -> {
                                order.setId(nextId);
                                return side == OrderSide.BID
                                        ? orderBook.placeBid(order)
                                        : orderBook.placeAsk(order);
                            });
                });
    }

    public Mono<Boolean> hasEnough(Account account, String assetId, long required) {
        synchronized (accountingLock) {
            return account.hasEnough(assetId, required);
        }
    }

    public Mono<Boolean> reserve(Account account, String assetId, long required) {
        synchronized (accountingLock) {
            return account.reserve(assetId, required);
        }
    }


    public Long getTime() {
        return System.currentTimeMillis();
    }

    public Mono<Long> advance() {
        final long timeNow = getTime();
        return Flux.fromIterable(orderBooks.entrySet())
                .flatMap(ob -> ob.getValue().matchMultiple(this, timeNow))
                .reduce(Long::sum)
                .flatMap(result -> {
                    time.incrementAndGet();
                    return Mono.just(result);
                });
    }

    public Flux<OrderBook> getOrderBooks() {
        return Flux.fromIterable(orderBooks.values().stream().toList());
    }
}
