package sim.stock_exchange.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Account {
    Map<Long, AtomicLong> assets = new ConcurrentHashMap<>();
    Map<Long, Long> overdraftLimits = new HashMap<>();
    long id;
    String extra = "";
    public boolean bankrupt = false;

    public Account(long id) {
        this.id = id;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public boolean transfer(long assetId, long volume) {
        if (!assets.containsKey(assetId)) {
            assets.put(assetId, new AtomicLong(0L));
        }
        AtomicLong value = assets.get(assetId);
        long result = value.addAndGet(volume);
        return result >= 0;
    }

    public long getId() {
        return id;
    }

    public long getVolume(long assetId) {
        if (!assets.containsKey(assetId)) return 0L;
        return assets.get(assetId).get();
    }

    public boolean hasEnough(long assetId, long required) {
        if (!assets.containsKey(assetId)) return false;
        return assets.get(assetId).get() >= required;
    }

    public Map<Long, Long> getPublicAssets() {
        Map<Long, Long> result = new HashMap<>();
        for (Map.Entry<Long, AtomicLong> entry : assets.entrySet()) {
            result.put(entry.getKey(), entry.getValue().get());
        }
        return result;
    }

    public boolean isBankrupt() {
        return bankrupt;
    }

    public void setOverdraftLimit(long assetId, long limit) {
        this.overdraftLimits.put(assetId, limit);
    }

    public long getOverdraftLimit(long assetId) {
        return overdraftLimits.getOrDefault(assetId, 0L);
    }

    public void setBankrupt(boolean bankrupt) {
        this.bankrupt = bankrupt;
    }
}
