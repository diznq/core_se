package exchange.core;

import exchange.constant.OrderSide;
import exchange.exc.InsufficientAssets;
import exchange.model.Account;
import exchange.model.Asset;
import exchange.model.Order;
import exchange.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class AssetManager {
    Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
    AtomicLong orderIdCounter = new AtomicLong(0L);
    AtomicLong time = new AtomicLong(0L);

    @Autowired
    AccountService accountService;

    public AssetManager() {

    }

    public Mono<Account> getAccount(String name) {
        return Mono.justOrEmpty(accountService.getAccountByName(name));
    }

    public Asset transferTo(Account account, String asset, long volume, String reason) {
        return accountService.transfer(account, asset, volume);
    }

    public Mono<Boolean> hasEnough(Account account, String assetId, long required) {
        return accountService.hasEnough(account, assetId, required);
    }

    public Asset reserve(Account account, String assetId, long required) {
        return accountService.reserve(account, assetId, required);
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
                    Asset reserveOk = reserve(order.getAccount(), lockAsset, lockVolume);
                    order.setId(nextId);
                    return side == OrderSide.BID
                            ? orderBook.placeBid(order)
                            : orderBook.placeAsk(order);
                });
    }

    @Transactional
    public Mono<Boolean> transaction(Order bidPeek, Order askPeek, String left, String right, long price,
                                     long transferVolume) {
        askPeek.fill(transferVolume);
        bidPeek.fill(transferVolume);
        Account bidderAccount = bidPeek.getAccount();
        Account askerAccount = askPeek.getAccount();
        transferTo(bidderAccount, right, -price * transferVolume, "bid-right-rem");
        reserve(bidderAccount, right, -price * transferVolume);
        transferTo(bidderAccount, left, transferVolume, "bid-left-add");

        transferTo(askerAccount, right, price * transferVolume, "ask-right-add");
        transferTo(askerAccount, left, -transferVolume, "ask-left-rem");
        reserve(askerAccount, left, -transferVolume);
        return Mono.just(true);
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

    public Map<String, Long> getPublicAssets(Account account) {
        return accountService.getPublicAssets(account);
    }
}
