package sim.stock_exchange.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AssetRepository {
    
    Map<Long, Account> accounts = new ConcurrentHashMap<>();
    Map<String, Long> ids = new ConcurrentHashMap<>();
    Map<Long, String> idsReverse = new ConcurrentHashMap<>();
    Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
    AtomicLong idCounter = new AtomicLong(0L);
    AtomicLong orderIdCounter = new AtomicLong(0L);
    AtomicLong time = new AtomicLong(0L);
    Object accountingLock = new Object();

    Logger log = LoggerFactory.getLogger(AssetRepository.class);

    public AssetRepository() {

    }


    public boolean transferTo(long accId, long assetId, long volume) {
        return transferTo(accId, assetId, volume, "unknown");
    }

    public boolean transferTo(long accId, long assetId, long volume, String reason) {
        synchronized(accountingLock){
            Account account = getAccount(accId);
            if (account == null) return false;
            boolean result = account.transfer(assetId, volume);
            if (!result && volume < 0L) {
                return false;
            }
            return true;
        }
    }

    public boolean transferTo(Account account, String asset, long volume) {
        return transferTo(account.getId(), getId(asset), volume);
    }

    public boolean transferTo(Account account, String asset, long volume, String reason) {
        return transferTo(account.getId(), getId(asset), volume, reason);
    }

    public Account getAccount(long accId) {
        if (accounts.containsKey(accId)) {
            return accounts.get(accId);
        }
        Account account = new Account(accId);
        accounts.put(accId, account);
        return account;
    }

    public Account getAccount(String name) {
        return getAccount(getId(name));
    }

    public long getId(String symbol) {
        if (ids.containsKey(symbol)) {
            return ids.get(symbol);
        }
        long res = idCounter.incrementAndGet();
        ids.put(symbol, res);
        idsReverse.put(res, symbol);
        return res;
    }

    public long getOrderId() {
        return orderIdCounter.getAndIncrement();
    }

    public String getSymbol(long id) {
        return idsReverse.get(id);
    }

    public OrderBook getOrderBook(String base, String quote) {
        String common = base + quote;
        long baseId = getId(base);
        long quoteId = getId(quote);
        if (orderBooks.containsKey(common)) {
            return orderBooks.get(common);
        }
        OrderBook book = new OrderBook(baseId, quoteId, common);
        orderBooks.put(common, book);
        return book;
    }

    public Order placeBid(OrderBook orderBook, Order order, long ttl) {
        if (!hasEnough(order.getAccount(), orderBook.getRight(), order.getVolume() * order.getPrice())) {
            return null;
        }
        order.setId(getOrderId());
        if (ttl != 0L) {
            order.setTtl(getTime() + ttl);
        }
        orderBook.placeBid(order);
        return order;
    }

    public Order placeAsk(OrderBook orderBook, Order order, long ttl) {
        if (!hasEnough(order.getAccount(), orderBook.getLeft(), order.getVolume())) {
            return null;
        }
        order.setId(getOrderId());
        if (ttl != 0L) {
            order.setTtl(getTime() + ttl);
        }
        orderBook.placeAsk(order);
        return order;
    }

    public boolean hasEnough(long accId, long assetId, long required) {
        synchronized(accountingLock){
            Account account = getAccount(accId);
            return account.hasEnough(assetId, required);
        }
    }

    public long getTime() {
        return time.get();
    }

    public long advance() {
        long matches = 0L;
        for (Map.Entry<String, OrderBook> ob : orderBooks.entrySet()) {
            matches += ob.getValue().tick(this);
        }
        time.incrementAndGet();
        return matches;
    }

    public List<OrderBook> getOrderBooks() {
        return orderBooks.values().stream().toList();
    }

    public long getOutstandingShares(long assetId, long accountId) {
        long total = 0;
        for (Account account : accounts.values()) {
            if (account.getId() == accountId) continue;
            total += account.getVolume(assetId);
        }
        return total;
    }

    public long getTotalAssets(Account acc, String currency) {
        long total = 0L;
        Map<Long, Long> assets = acc.getPublicAssets();
        for (Map.Entry<Long, Long> asset : assets.entrySet()) {
            if (getSymbol(asset.getKey()).equals(currency)) {
                total += asset.getValue();
                continue;
            }
            OrderBook orderBook = getOrderBook(getSymbol(asset.getKey()), currency);
            total += asset.getValue() * orderBook.getLastPrice();
        }
        return total;
    }
}
